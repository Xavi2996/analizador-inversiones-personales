import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ReferidoFormComponent } from './referido-form.component';
import { ListaReferidos } from './referido.model';
import { ReferidoService } from './referido.service';

type EstadoCarga = 'cargando' | 'listo' | 'error';

/** Tiempo que el aviso permanece visible (guía §39: desaparece sin bloquear). */
const DURACION_AVISO_MS = 4000;

/**
 * Pantalla Referidos: total del valor al recibir e historial.
 * El total y el conteo vienen del backend; el frontend no suma la lista (spec 004 §3.6).
 * Solo datos históricos: valor actual y variación llegan con Precios.
 */
@Component({
  selector: 'app-referidos-pagina',
  imports: [CurrencyPipe, DecimalPipe, ReferidoFormComponent],
  templateUrl: './referidos-pagina.component.html',
  styleUrl: './referidos-pagina.component.css',
})
export class ReferidosPaginaComponent implements OnInit {
  private readonly referidoService = inject(ReferidoService);

  protected readonly estado = signal<EstadoCarga>('cargando');
  protected readonly lista = signal<ListaReferidos | null>(null);
  protected readonly formularioAbierto = signal(false);
  protected readonly aviso = signal<string | null>(null);

  private temporizadorAviso?: ReturnType<typeof setTimeout>;

  constructor() {
    inject(DestroyRef).onDestroy(() => clearTimeout(this.temporizadorAviso));
  }

  ngOnInit(): void {
    this.cargar();
  }

  /** Con recarga silenciosa (después de registrar) el historial se mantiene visible mientras se consulta. */
  protected cargar(silenciosa = false): void {
    if (!silenciosa) {
      this.estado.set('cargando');
    }
    this.referidoService.listar().subscribe({
      next: (lista) => {
        this.lista.set(lista);
        this.estado.set('listo');
      },
      error: () => this.estado.set('error'),
    });
  }

  protected abrirRegistro(): void {
    this.formularioAbierto.set(true);
  }

  /** Cierra el panel, confirma el registro y recarga historial, total y conteo. */
  protected alGuardar(): void {
    this.formularioAbierto.set(false);
    clearTimeout(this.temporizadorAviso);
    this.aviso.set('Referido registrado correctamente.');
    this.temporizadorAviso = setTimeout(() => this.aviso.set(null), DURACION_AVISO_MS);
    this.cargar(true);
  }

  protected referidos(conteo: number): string {
    return `${conteo} ${conteo === 1 ? 'referido' : 'referidos'}`;
  }

  /** AAAA-MM-DD (API) → DD/MM/AAAA (guía §49). Sin pasar por Date para no depender de la zona horaria. */
  protected fecha(fechaApi: string): string {
    const [anio, mes, dia] = fechaApi.split('-');
    return `${dia}/${mes}/${anio}`;
  }
}
