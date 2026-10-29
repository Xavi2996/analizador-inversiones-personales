import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { LayoutComponent } from './layout.component';

@Component({ template: '<h1>Pantalla de prueba</h1>' })
class PantallaPrueba {}

describe('LayoutComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LayoutComponent],
      providers: [provideRouter([{ path: 'activos', component: PantallaPrueba }])],
    }).compileComponents();
  });

  async function crear() {
    await TestBed.inject(Router).navigateByUrl('/activos');
    const fixture = TestBed.createComponent(LayoutComponent);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, elemento: fixture.nativeElement as HTMLElement };
  }

  it('muestra solo la entrada Activos en la navegación lateral', async () => {
    const { elemento } = await crear();
    const enlaces = elemento.querySelectorAll('.lateral-nav a');
    expect(enlaces.length).toBe(1);
    expect(enlaces[0].textContent?.trim()).toBe('Activos');
    expect(enlaces[0].getAttribute('href')).toBe('/activos');
  });

  it('marca como actual la pantalla activa', async () => {
    const { elemento } = await crear();
    const enlace = elemento.querySelector('.lateral-nav a');
    expect(enlace?.getAttribute('aria-current')).toBe('page');
    expect(elemento.querySelector('.pantalla')?.textContent).toBe('Activos');
  });

  it('abre y cierra el menú móvil', async () => {
    const { fixture, elemento } = await crear();
    const boton = elemento.querySelector<HTMLButtonElement>('.boton-menu')!;
    expect(boton.getAttribute('aria-expanded')).toBe('false');
    expect(elemento.querySelector('#menu-movil')).toBeNull();

    boton.click();
    fixture.detectChanges();
    expect(boton.getAttribute('aria-expanded')).toBe('true');
    expect(elemento.querySelector('#menu-movil a')?.textContent?.trim()).toBe('Activos');

    elemento.querySelector<HTMLAnchorElement>('#menu-movil a')!.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(elemento.querySelector('#menu-movil')).toBeNull();
  });

  it('muestra la pantalla de la ruta dentro del contenido principal', async () => {
    const { elemento } = await crear();
    expect(elemento.querySelector('main#contenido h1')?.textContent).toBe('Pantalla de prueba');
  });
});
