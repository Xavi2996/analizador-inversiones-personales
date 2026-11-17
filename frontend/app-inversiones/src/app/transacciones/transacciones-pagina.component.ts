import { CurrencyPipe, DecimalPipe, NgTemplateOutlet } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { Subscription } from 'rxjs';
import { Activo } from '../activos/activo.model';
import { ActivoService } from '../activos/activo.service';
import { ListaTransacciones, TipoTransaccion } from './transaccion.model';
import { TransaccionFormComponent } from './transaccion-form.component';
import { TransaccionService } from './transaccion.service';

type EstadoCarga = 'cargando' | 'listo' | 'error';

/** Tiempo que el aviso permanece visible (guía §39: desaparece sin bloquear). */
const DURACION_AVISO_MS = 4000;

/** Pestañas Todas / Compras / Ventas; `null` es "Todas". */
interface Pestana {
  tipo: TipoTransaccion | null;
  etiqueta: string;
}

const PESTANAS: Pestana[] = [
  { tipo: null, etiqueta: 'Todas' },
  { tipo: 'COMPRA', etiqueta: 'Compras' },
  { tipo: 'VENTA', etiqueta: 'Ventas' },
];

/**
 * Pantalla Transacciones: historial con filtros por tipo y activo, y totales.
 * Los filtros se aplican en la API; totales y conteos vienen del backend y el frontend no suma la lista
 * (spec 003 §3.6).
 */
@Component({
  selector: 'app-transacciones-pagina',
  imports: [CurrencyPipe, DecimalPipe, NgTemplateOutlet, TransaccionFormComponent],
  templateUrl: './transacciones-pagina.component.html',
  styleUrl: './transacciones-pagina.component.css',
})
export class TransaccionesPaginaComponent implements OnInit {
  private readonly transaccionService = inject(TransaccionService);
  private readonly activoService = inject(ActivoService);

  protected readonly pestanas = PESTANAS;

  protected readonly estado = signal<EstadoCarga>('cargando');
  protected readonly lista = signal<ListaTransacciones | null>(null);
  protected readonly activos = signal<Activo[]>([]);
  protected readonly tipo = signal<TipoTransaccion | null>(null);
  protected readonly activoId = signal<number | null>(null);
  protected readonly formularioAbierto = signal(false);
  protected readonly aviso = signal<string | null>(null);

  /** Sin filtros y sin transacciones: estado vacío de la pantalla, sin filtros ni historial. */
  protected readonly sinTransacciones = computed(() => {
    const datos = this.lista();
    return (
      datos !== null &&
      this.activoId() === null &&
      this.tipo() === null &&
      datos.conteoCompras + datos.conteoVentas === 0
    );
  });

  /** Con compras y ventas en la lista, el pie muestra un total por tipo y las comisiones aparte. */
  protected readonly totalesPorTipo = computed(() => {
    const datos = this.lista();
    return this.tipo() === null && datos !== null && datos.conteoCompras > 0 && datos.conteoVentas > 0;
  });

  /** Tipo de la única fila de total cuando la lista tiene un solo tipo. */
  protected readonly tipoTotalUnico = computed<TipoTransaccion>(() => {
    const datos = this.lista();
    if (this.tipo() !== null) {
      return this.tipo()!;
    }
    return datos !== null && datos.conteoCompras === 0 && datos.conteoVentas > 0 ? 'VENTA' : 'COMPRA';
  });

  private consulta?: Subscription;
  private temporizadorAviso?: ReturnType<typeof setTimeout>;

  constructor() {
    inject(DestroyRef).onDestroy(() => {
      this.consulta?.unsubscribe();
      clearTimeout(this.temporizadorAviso);
    });
  }

  ngOnInit(): void {
    this.cargar();
    this.activoService.listar().subscribe({
      next: (activos) => this.activos.set(activos),
      // Sin activos el selector solo ofrece "Todos los activos"; el historial sigue disponible.
      error: () => this.activos.set([]),
    });
  }

  /**
   * Consulta la API con los filtros actuales. Una consulta nueva cancela la anterior para que una
   * respuesta lenta no pise a la más reciente. Al filtrar, el historial anterior queda visible.
   */
  protected cargar(): void {
    this.consulta?.unsubscribe();
    this.estado.set('cargando');
    this.consulta = this.transaccionService
      .listar({ activoId: this.activoId(), tipo: this.tipo() })
      .subscribe({
        next: (lista) => {
          this.lista.set(lista);
          this.estado.set('listo');
        },
        error: () => this.estado.set('error'),
      });
  }

  protected seleccionarTipo(tipo: TipoTransaccion | null): void {
    if (tipo === this.tipo()) {
      return;
    }
    this.tipo.set(tipo);
    this.cargar();
  }

  protected seleccionarActivo(valor: string): void {
    this.activoId.set(valor === '' ? null : Number(valor));
    this.cargar();
  }

  /** Flechas, Inicio y Fin mueven y activan la pestaña (patrón ARIA tablist). */
  protected moverPestana(evento: KeyboardEvent, indice: number): void {
    const ultimo = PESTANAS.length - 1;
    const destino: Record<string, number> = {
      ArrowRight: indice === ultimo ? 0 : indice + 1,
      ArrowLeft: indice === 0 ? ultimo : indice - 1,
      Home: 0,
      End: ultimo,
    };
    const nuevo = destino[evento.key];
    if (nuevo === undefined) {
      return;
    }
    evento.preventDefault();
    this.seleccionarTipo(PESTANAS[nuevo].tipo);
    const lista = (evento.currentTarget as HTMLElement).parentElement;
    lista?.querySelectorAll<HTMLButtonElement>('[role="tab"]')[nuevo]?.focus();
  }

  protected conteo(tipo: TipoTransaccion | null, datos: ListaTransacciones): number {
    if (tipo === 'COMPRA') {
      return datos.conteoCompras;
    }
    if (tipo === 'VENTA') {
      return datos.conteoVentas;
    }
    return datos.conteoCompras + datos.conteoVentas;
  }

  protected abrirRegistro(): void {
    this.formularioAbierto.set(true);
  }

  /** Cierra el panel, confirma el registro y recarga historial, totales y conteos con los filtros actuales. */
  protected alGuardar(tipo: TipoTransaccion): void {
    this.formularioAbierto.set(false);
    clearTimeout(this.temporizadorAviso);
    this.aviso.set(tipo === 'COMPRA' ? 'Compra registrada correctamente.' : 'Venta registrada correctamente.');
    this.temporizadorAviso = setTimeout(() => this.aviso.set(null), DURACION_AVISO_MS);
    this.cargar();
  }

  /** Mensaje cuando los filtros no devuelven transacciones pero sí existen otras. */
  protected mensajeSinResultados(): string {
    const tipo = this.tipo();
    if (tipo === 'COMPRA') {
      return 'No hay compras con este filtro.';
    }
    if (tipo === 'VENTA') {
      return 'No hay ventas con este filtro.';
    }
    return 'No hay transacciones de este activo.';
  }

  /** AAAA-MM-DD (API) → DD/MM/AAAA (guía §49). Sin pasar por Date para no depender de la zona horaria. */
  protected fecha(fechaApi: string): string {
    const [anio, mes, dia] = fechaApi.split('-');
    return `${dia}/${mes}/${anio}`;
  }
}
