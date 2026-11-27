import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ListaReferidos, Referido } from './referido.model';
import { ReferidosPaginaComponent } from './referidos-pagina.component';

/** Referido de la hoja Referidos del Excel. */
const REFERIDO_EXCEL: Referido = {
  id: 1,
  activoId: 3,
  simbolo: 'BTC',
  nombreActivo: 'Bitcoin',
  fechaReferido: '2026-10-13',
  origen: 'Programa de referidos Hapi',
  cantidad: 0.00001196,
  precioAlRecibir: 83580.27,
  valorRecibido: 1.0,
  comentario: null,
};

const LISTA: ListaReferidos = {
  referidos: [
    {
      ...REFERIDO_EXCEL,
      id: 2,
      fechaReferido: '2026-10-20',
      origen: 'Programa de referidos Binance',
      cantidad: 0.5,
      precioAlRecibir: 6,
      valorRecibido: 3.0,
      comentario: 'Bono de bienvenida',
    },
    REFERIDO_EXCEL,
  ],
  totalValorRecibido: 4.0,
  conteo: 2,
};

const LISTA_VACIA: ListaReferidos = { referidos: [], totalValorRecibido: 0, conteo: 0 };

describe('ReferidosPaginaComponent', () => {
  let fixture: ComponentFixture<ReferidosPaginaComponent>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReferidosPaginaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ReferidosPaginaComponent);
    elemento = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function responder(lista: ListaReferidos): void {
    http.expectOne('/api/v1/referidos').flush(lista);
    fixture.detectChanges();
  }

  function texto(selector: string): string {
    return elemento.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  }

  function textos(selector: string): string[] {
    return Array.from(elemento.querySelectorAll(selector)).map(
      (nodo) => nodo.textContent?.replace(/\s+/g, ' ').trim() ?? '',
    );
  }

  it('muestra el estado de carga mientras consulta la API', () => {
    expect(texto('[role="status"]')).toBe('Cargando referidos…');
    responder(LISTA);
  });

  it('muestra el estado vacío con la acción de registrar el primer referido', () => {
    responder(LISTA_VACIA);

    expect(texto('.vacio p')).toBe('No existen referidos registrados.');
    expect(texto('.vacio button')).toBe('Registrar primer referido');
    expect(elemento.querySelector('table')).toBeNull();
    expect(elemento.querySelector('.totales')).toBeNull();
    expect(elemento.querySelector('.encabezado button')).toBeNull();
  });

  it('muestra un error comprensible y permite reintentar', () => {
    http.expectOne('/api/v1/referidos').flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(texto('[role="alert"] p')).toContain('No fue posible cargar los referidos.');
    elemento.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    fixture.detectChanges();

    expect(texto('[role="status"]')).toBe('Cargando referidos…');
    responder(LISTA);
    expect(elemento.querySelectorAll('tbody tr').length).toBe(2);
  });

  it('muestra el valor al recibir y el conteo del backend sin sumar la lista', () => {
    // Total y conteo deliberadamente distintos de la lista: deben mostrarse tal cual.
    responder({ ...LISTA, totalValorRecibido: 999.99, conteo: 7 });

    expect(texto('.totales .etiqueta')).toBe('Valor al recibir');
    expect(texto('.cifra-principal')).toBe('$999.99');
    expect(texto('.cifra-nota')).toBe('Histórico · 7 referidos');
    expect(texto('tfoot')).toContain('$999.99');
  });

  it('usa el singular con un solo referido', () => {
    responder({ referidos: [REFERIDO_EXCEL], totalValorRecibido: 1.0, conteo: 1 });

    expect(texto('.cifra-nota')).toBe('Histórico · 1 referido');
    expect(texto('.historial-nota')).toBe('1 referido · más reciente primero');
  });

  it('lista el historial en el orden del backend solo con columnas históricas', () => {
    responder(LISTA);

    expect(texto('thead th.grupo')).toBe('Al recibir · histórico');
    const encabezados = textos('thead tr:last-child th');
    expect(encabezados).toEqual(['Fecha y origen', 'Activo', 'Cantidad × precio', 'Valor recibido']);
    // Valor actual, variación y rendimiento llegan con Precios.
    expect(texto('thead')).not.toContain('Hoy');
    expect(texto('thead')).not.toContain('Valor actual');

    const filas = elemento.querySelectorAll('tbody tr');
    expect(filas.length).toBe(2);
    const primera = filas[0].textContent!.replace(/\s+/g, ' ');
    expect(primera).toContain('20/10/2026');
    expect(primera).toContain('Programa de referidos Binance');
    expect(primera).toContain('Bono de bienvenida');

    const segunda = filas[1].textContent!.replace(/\s+/g, ' ');
    expect(segunda).toContain('13/10/2026');
    expect(segunda).toContain('Programa de referidos Hapi');
    expect(segunda).toContain('BTC');
    expect(segunda).toContain('0.00001196');
    expect(segunda).toContain('× $83,580.27');
    expect(segunda).toContain('$1.00');
    expect(filas[1].querySelector('.comentario')).toBeNull();

    expect(textos('tfoot th, tfoot td')).toEqual(['Total', '$4.00']);
    expect(texto('.historial-nota')).toBe('2 referidos · más reciente primero');
  });

  it('conserva los decimales del precio al recibir', () => {
    responder({
      referidos: [{ ...REFERIDO_EXCEL, precioAlRecibir: 0.123456, cantidad: 10, valorRecibido: 1.23 }],
      totalValorRecibido: 1.23,
      conteo: 1,
    });

    expect(texto('tbody .precio')).toBe('× $0.123456');
    expect(texto('tbody .cantidad')).toBe('10.00000000');
  });

  it('incluye la lista compacta para móvil con el total', () => {
    responder(LISTA);

    const tarjetas = elemento.querySelectorAll('.tarjetas li:not(.tarjeta-total)');
    expect(tarjetas.length).toBe(2);
    const segunda = tarjetas[1].textContent!.replace(/\s+/g, ' ');
    expect(segunda).toContain('BTC');
    expect(segunda).toContain('13/10/2026 · Programa de referidos Hapi');
    expect(segunda).toContain('Al recibir · histórico');
    expect(segunda).toContain('0.00001196');
    expect(segunda).toContain('× $83,580.27');
    expect(segunda).toContain('$1.00');
    expect(textos('.tarjeta-total span')).toEqual(['Total', '$4.00']);
  });

  it('muestra la acción Registrar referido cuando hay referidos', () => {
    responder(LISTA);

    expect(texto('.encabezado button')).toBe('Registrar referido');
  });

  it('abre el formulario desde el estado vacío y lo cierra al cancelar', () => {
    responder(LISTA_VACIA);

    elemento.querySelector<HTMLButtonElement>('.vacio button')!.click();
    fixture.detectChanges();
    expect(elemento.querySelector('app-referido-form')).not.toBeNull();
    http.expectOne('/api/v1/activos').flush([]);

    fixture.debugElement.query((nodo) => nodo.name === 'app-referido-form').triggerEventHandler('cerrado');
    fixture.detectChanges();
    expect(elemento.querySelector('app-referido-form')).toBeNull();
  });

  it('al registrar cierra el panel, confirma y recarga sin ocultar el historial', () => {
    responder(LISTA);
    elemento.querySelector<HTMLButtonElement>('.encabezado button')!.click();
    fixture.detectChanges();
    http.expectOne('/api/v1/activos').flush([]);

    // El formulario emite al registrar (sus pruebas cubren el envío).
    fixture.debugElement.query((nodo) => nodo.name === 'app-referido-form').triggerEventHandler('guardado');
    fixture.detectChanges();

    expect(elemento.querySelector('app-referido-form')).toBeNull();
    expect(texto('.aviso')).toBe('✓ Referido registrado correctamente.');
    // Recarga silenciosa: el historial sigue visible mientras llega la respuesta.
    expect(elemento.querySelectorAll('tbody tr').length).toBe(2);

    responder({ ...LISTA, totalValorRecibido: 5.0, conteo: 3 });
    expect(texto('.cifra-principal')).toBe('$5.00');
  });
});
