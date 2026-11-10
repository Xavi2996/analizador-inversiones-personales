import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { Aporte, ListaAportes } from './aporte.model';
import { AporteService } from './aporte.service';

type EstadoCarga = 'cargando' | 'listo' | 'error';

/**
 * Pantalla Aportes: totales, barra neto/costo e historial.
 * Totales y porcentajes vienen del backend; el frontend no suma la lista (spec 002 §5).
 */
@Component({
  selector: 'app-aportes-pagina',
  imports: [CurrencyPipe, DecimalPipe],
  templateUrl: './aportes-pagina.component.html',
  styleUrl: './aportes-pagina.component.css',
})
export class AportesPaginaComponent implements OnInit {
  private readonly aporteService = inject(AporteService);

  protected readonly estado = signal<EstadoCarga>('cargando');
  protected readonly lista = signal<ListaAportes | null>(null);
  /** El formulario de registro se incorpora en el paso 7 de la spec. */
  protected readonly formularioAbierto = signal(false);

  ngOnInit(): void {
    this.cargar();
  }

  /** Con recarga silenciosa (después de registrar) el historial se mantiene visible mientras se consulta. */
  protected cargar(silenciosa = false): void {
    if (!silenciosa) {
      this.estado.set('cargando');
    }
    this.aporteService.listar().subscribe({
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

  /** Línea de la lista móvil: método y comentario presentes, separados por " · ". */
  protected metodoYComentario(aporte: Aporte): string {
    return [aporte.metodoFondeo, aporte.comentario].filter((texto) => !!texto).join(' · ');
  }

  /** AAAA-MM-DD (API) → DD/MM/AAAA (guía §49). Sin pasar por Date para no depender de la zona horaria. */
  protected fecha(fechaApi: string): string {
    const [anio, mes, dia] = fechaApi.split('-');
    return `${dia}/${mes}/${anio}`;
  }
}
