import { DecimalPipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { ActivoFormComponent, PanelActivo, ResultadoGuardado } from './activo-form.component';
import { Activo, ListaAsignaciones, TipoActivo } from './activo.model';
import { ActivoService } from './activo.service';
import { AsignacionService } from './asignacion.service';

type EstadoCarga = 'cargando' | 'listo' | 'error';

/** Tiempo que el aviso permanece visible (guía §39: desaparece sin bloquear). */
const DURACION_AVISO_MS = 4000;

const ETIQUETAS_TIPO: Record<TipoActivo, string> = {
  ACCION: 'Acción',
  ETF: 'ETF',
  CRIPTOMONEDA: 'Criptomoneda',
};

/**
 * Franjas de color que identifican cada activo en la tabla y en la barra de asignación.
 * Solo son presentación: se asignan por orden de símbolo y se repiten si hay más activos que colores.
 */
const COLORES_ACTIVO = ['#3b82f6', '#38bdf8', '#cbd5e1', '#64748b'];

interface FilaActivo {
  activo: Activo;
  tipo: string;
  color: string;
  /** null cuando el activo no tiene asignación registrada (se muestra "—"). */
  objetivo: number | null;
}

interface SegmentoAsignacion {
  simbolo: string;
  porcentaje: number;
  color: string;
}

@Component({
  selector: 'app-activos-pagina',
  imports: [DecimalPipe, ActivoFormComponent],
  templateUrl: './activos-pagina.component.html',
  styleUrl: './activos-pagina.component.css',
})
export class ActivosPaginaComponent implements OnInit {
  private readonly activoService = inject(ActivoService);
  private readonly asignacionService = inject(AsignacionService);

  protected readonly estado = signal<EstadoCarga>('cargando');
  protected readonly activos = signal<Activo[]>([]);
  protected readonly asignaciones = signal<ListaAsignaciones | null>(null);
  protected readonly panel = signal<PanelActivo | null>(null);
  protected readonly aviso = signal<ResultadoGuardado | null>(null);

  private temporizadorAviso?: ReturnType<typeof setTimeout>;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.temporizadorAviso));
  }

  private readonly colorPorActivo = computed(
    () => new Map(this.activos().map((a, i) => [a.id, COLORES_ACTIVO[i % COLORES_ACTIVO.length]])),
  );

  protected readonly filas = computed<FilaActivo[]>(() => {
    const objetivos = new Map(
      (this.asignaciones()?.asignaciones ?? []).map((a) => [a.activoId, a.porcentajeObjetivo]),
    );
    return this.activos().map((activo) => ({
      activo,
      tipo: ETIQUETAS_TIPO[activo.tipoActivo],
      color: this.colorPorActivo().get(activo.id)!,
      objetivo: objetivos.get(activo.id) ?? null,
    }));
  });

  protected readonly segmentos = computed<SegmentoAsignacion[]>(() =>
    (this.asignaciones()?.asignaciones ?? [])
      .filter((a) => a.porcentajeObjetivo > 0)
      .map((a) => ({
        simbolo: a.simbolo,
        porcentaje: a.porcentajeObjetivo,
        color: this.colorPorActivo().get(a.activoId) ?? COLORES_ACTIVO[0],
      })),
  );

  /**
   * Parte de la barra que queda sin asignar cuando la suma es menor que 100.
   * Es solo la proporción visual de la barra; la suma y su validez vienen del backend.
   */
  protected readonly restoSinAsignar = computed(() => {
    const suma = this.asignaciones()?.sumaPorcentajes ?? 0;
    return suma < 100 ? 100 - suma : 0;
  });

  ngOnInit(): void {
    this.cargar();
  }

  /** Con recarga silenciosa (después de guardar) la tabla se mantiene visible mientras se consulta. */
  protected cargar(silenciosa = false): void {
    if (!silenciosa) {
      this.estado.set('cargando');
    }
    forkJoin({
      activos: this.activoService.listar(),
      asignaciones: this.asignacionService.listar(),
    }).subscribe({
      next: ({ activos, asignaciones }) => {
        this.activos.set(activos);
        this.asignaciones.set(asignaciones);
        this.estado.set('listo');
      },
      error: () => this.estado.set('error'),
    });
  }

  protected abrirRegistro(): void {
    this.panel.set({ modo: 'crear' });
  }

  protected abrirEdicion(activo: Activo): void {
    this.panel.set({ modo: 'editar', activo });
  }

  /** Porcentaje objetivo registrado del activo que se edita, para precargar el formulario. */
  protected porcentajeDe(panel: PanelActivo): number | null {
    if (panel.modo !== 'editar') {
      return null;
    }
    return this.filas().find((f) => f.activo.id === panel.activo.id)?.objetivo ?? null;
  }

  protected alGuardar(resultado: ResultadoGuardado): void {
    this.panel.set(null);
    this.mostrarAviso(resultado);
    this.cargar(true);
  }

  private mostrarAviso(resultado: ResultadoGuardado): void {
    clearTimeout(this.temporizadorAviso);
    this.aviso.set(resultado);
    this.temporizadorAviso = setTimeout(() => this.aviso.set(null), DURACION_AVISO_MS);
  }
}
