import { DecimalPipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { Activo, ListaAsignaciones, TipoActivo } from './activo.model';
import { ActivoService } from './activo.service';
import { AsignacionService } from './asignacion.service';

type EstadoCarga = 'cargando' | 'listo' | 'error';

/** Panel lateral de registro o edición (se implementa en el paso 11 de la spec 001). */
export type PanelActivo = { modo: 'crear' } | { modo: 'editar'; activo: Activo };

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
  imports: [DecimalPipe],
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

  protected cargar(): void {
    this.estado.set('cargando');
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
}
