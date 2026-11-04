import { NgTemplateOutlet } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';

interface ItemNavegacion {
  ruta: string;
  etiqueta: string;
  /** Trazo SVG (viewBox 24×24) del icono. */
  icono: string;
}

interface GrupoNavegacion {
  titulo: string;
  items: ItemNavegacion[];
}

/**
 * Solo se muestran las pantallas implementadas; cada spec agrega su entrada (spec 001 §6).
 */
const GRUPOS_NAVEGACION: GrupoNavegacion[] = [
  {
    titulo: 'Registro',
    items: [{ ruta: '/activos', etiqueta: 'Activos', icono: 'M4 7h16v13H4zM8 7V4h8v3' }],
  },
];

/**
 * Shell de la aplicación: navegación lateral en escritorio y barra superior con menú en móvil.
 * El cambio entre ambas se resuelve con CSS (punto de corte 880 px, igual que el prototipo).
 */
@Component({
  selector: 'app-layout',
  imports: [NgTemplateOutlet, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css',
})
export class LayoutComponent {
  private readonly router = inject(Router);

  protected readonly grupos = GRUPOS_NAVEGACION;
  protected readonly menuAbierto = signal(false);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((evento) => evento instanceof NavigationEnd),
      map((evento) => evento.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  /** Nombre de la pantalla actual para la barra superior móvil. */
  protected readonly pantallaActual = computed(() => {
    const url = this.url();
    const item = this.grupos
      .flatMap((grupo) => grupo.items)
      .find((i) => url === i.ruta || url.startsWith(i.ruta + '/'));
    return item?.etiqueta ?? 'Analizador';
  });

  protected alternarMenu(): void {
    this.menuAbierto.update((abierto) => !abierto);
  }

  protected cerrarMenu(): void {
    this.menuAbierto.set(false);
  }

  protected saltarAlContenido(evento: Event, contenido: HTMLElement): void {
    evento.preventDefault();
    contenido.focus();
  }
}
