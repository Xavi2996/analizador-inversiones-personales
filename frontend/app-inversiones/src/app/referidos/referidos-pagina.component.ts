import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ListaReferidos } from './referido.model';
import { ReferidoService } from './referido.service';

type EstadoCarga = 'cargando' | 'listo' | 'error';

/**
 * Pantalla Referidos: total del valor al recibir e historial.
 * El total y el conteo vienen del backend; el frontend no suma la lista (spec 004 §3.6).
 * Solo datos históricos: valor actual y variación llegan con Precios.
 */
@Component({
  selector: 'app-referidos-pagina',
  imports: [CurrencyPipe, DecimalPipe],
  templateUrl: './referidos-pagina.component.html',
  styleUrl: './referidos-pagina.component.css',
})
export class ReferidosPaginaComponent implements OnInit {
  private readonly referidoService = inject(ReferidoService);

  protected readonly estado = signal<EstadoCarga>('cargando');
  protected readonly lista = signal<ListaReferidos | null>(null);
  protected readonly formularioAbierto = signal(false);

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

  protected referidos(conteo: number): string {
    return `${conteo} ${conteo === 1 ? 'referido' : 'referidos'}`;
  }

  /** AAAA-MM-DD (API) → DD/MM/AAAA (guía §49). Sin pasar por Date para no depender de la zona horaria. */
  protected fecha(fechaApi: string): string {
    const [anio, mes, dia] = fechaApi.split('-');
    return `${dia}/${mes}/${anio}`;
  }
}
