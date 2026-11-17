import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Activo } from '../activos/activo.model';
import { ListaTransacciones, Transaccion } from './transaccion.model';
import { TransaccionesPaginaComponent } from './transacciones-pagina.component';

const ACTIVOS: Activo[] = [
  { id: 3, simbolo: 'BTC', nombre: 'Bitcoin', tipoActivo: 'CRIPTOMONEDA', moneda: 'USD', simboloEditable: false },
  { id: 1, simbolo: 'IVV', nombre: 'iShares Core S&P 500 ETF', tipoActivo: 'ETF', moneda: 'USD', simboloEditable: false },
];

function transaccion(parcial: Partial<Transaccion>): Transaccion {
  return {
    id: 1,
    activoId: 1,
    simbolo: 'IVV',
    nombreActivo: 'iShares Core S&P 500 ETF',
    tipoTransaccion: 'COMPRA',
    fechaTransaccion: '2026-10-22',
    cantidad: 0.14553,
    precioUnitario: 769.57,
    montoBruto: 112.0,
    comision: 0.15,
    efectivoNeto: -112.15,
    comentario: null,
    ...parcial,
  };
}

/** Compras del Excel del 22/10; los totales son los de las 7 compras para comprobar que no se suma la lista. */
const LISTA_COMPRAS: ListaTransacciones = {
  transacciones: [
    transaccion({ id: 7, activoId: 3, simbolo: 'BTC', cantidad: 0.00012254, precioUnitario: 81606, montoBruto: 10.0, comision: 0.1 }),
    transaccion({ id: 5 }),
  ],
  totalComprado: 503.07,
  totalVendido: 0,
  totalComisiones: 1.02,
  conteoCompras: 7,
  conteoVentas: 0,
};

const LISTA_MIXTA: ListaTransacciones = {
  transacciones: [
    transaccion({ id: 9, tipoTransaccion: 'VENTA', fechaTransaccion: '2026-10-25', cantidad: 0.1, precioUnitario: 780, montoBruto: 78.0, comision: 0.15, efectivoNeto: 77.85 }),
    transaccion({ id: 5 }),
  ],
  totalComprado: 112.0,
  totalVendido: 78.0,
  totalComisiones: 0.3,
  conteoCompras: 1,
  conteoVentas: 1,
};

const LISTA_VACIA: ListaTransacciones = {
  transacciones: [],
  totalComprado: 0,
  totalVendido: 0,
  totalComisiones: 0,
  conteoCompras: 0,
  conteoVentas: 0,
};

describe('TransaccionesPaginaComponent', () => {
  let fixture: ComponentFixture<TransaccionesPaginaComponent>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransaccionesPaginaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TransaccionesPaginaComponent);
    elemento = fixture.nativeElement;
    fixture.detectChanges();
    http.expectOne('/api/v1/activos').flush(ACTIVOS);
  });

  afterEach(() => http.verify());

  function consulta(): TestRequest {
    return http.expectOne((r) => r.url === '/api/v1/transacciones');
  }

  function responder(lista: ListaTransacciones): TestRequest {
    const solicitud = consulta();
    solicitud.flush(lista);
    fixture.detectChanges();
    return solicitud;
  }

  function texto(selector: string): string {
    return elemento.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  }

  function textos(selector: string): string[] {
    return Array.from(elemento.querySelectorAll(selector)).map(
      (nodo) => nodo.textContent?.replace(/\s+/g, ' ').trim() ?? '',
    );
  }

  /** Texto de cada celda (th/td) no vacía de cada fila, separado por " | ". */
  function filas(selector: string): string[] {
    return Array.from(elemento.querySelectorAll(selector)).map((fila) =>
      Array.from(fila.children)
        .map((celda) => celda.textContent?.replace(/\s+/g, ' ').trim() ?? '')
        .filter((celda) => celda !== '')
        .join(' | '),
    );
  }

  function pestanas(): HTMLButtonElement[] {
    return Array.from(elemento.querySelectorAll<HTMLButtonElement>('[role="tab"]'));
  }

  it('muestra el estado de carga mientras consulta la API', () => {
    expect(texto('[role="status"]')).toBe('Cargando transacciones…');
    responder(LISTA_COMPRAS);
    expect(elemento.querySelector('[role="status"]')).toBeNull();
  });

  it('consulta sin filtros al abrir la pantalla', () => {
    const solicitud = responder(LISTA_COMPRAS);
    expect(solicitud.request.params.keys()).toEqual([]);
  });

  it('muestra el estado vacío con la acción de registrar la primera transacción', () => {
    responder(LISTA_VACIA);

    expect(texto('.vacio p')).toBe('No existen transacciones registradas.');
    expect(texto('.vacio button')).toBe('Registrar primera transacción');
    expect(elemento.querySelector('table')).toBeNull();
    expect(elemento.querySelector('[role="tablist"]')).toBeNull();
  });

  it('muestra un error comprensible y permite reintentar', () => {
    consulta().flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(texto('[role="alert"] p')).toContain('No fue posible cargar las transacciones.');
    elemento.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    fixture.detectChanges();

    expect(texto('[role="status"]')).toBe('Cargando transacciones…');
    responder(LISTA_COMPRAS);
    expect(elemento.querySelectorAll('tbody tr').length).toBe(2);
  });

  it('muestra cada transacción con fecha DD/MM/AAAA, cantidad de 8 decimales, precio y montos en USD', () => {
    responder(LISTA_COMPRAS);

    expect(filas('tbody tr')[1]).toBe('22/10/2026 | +IVVCompra | 0.14553000 | $769.57 | $0.15 | $112.00');
    expect(texto('tbody tr:nth-child(2) .simbolo')).toBe('IVV');
    expect(texto('tbody tr:nth-child(2) .tipo')).toBe('Compra');
    expect(texto('tbody tr:first-child td:nth-child(4)')).toBe('$81,606.00');
    expect(texto('tbody tr:first-child td:nth-child(3)')).toBe('0.00012254');
  });

  it('identifica la venta con signo y texto, no solo con color', () => {
    responder(LISTA_MIXTA);

    expect(texto('tbody tr:first-child .icono-tipo')).toBe('−');
    expect(texto('tbody tr:first-child .tipo')).toBe('Venta');
    expect(texto('.tarjetas li:first-child .tipo')).toBe('Venta · 25/10/2026');
  });

  it('muestra en el pie los totales del backend, sin sumar la lista', () => {
    responder(LISTA_COMPRAS);

    expect(filas('tfoot tr')).toEqual(['Total compras | $1.02 | $503.07']);
    expect(texto('.tarjeta-total > span:first-child')).toBe('Total compras');
    expect(texto('.tarjeta-total .total-principal')).toBe('$503.07');
    expect(texto('.tarjeta-total-detalle')).toBe('Comisión $1.02');
  });

  it('con compras y ventas muestra un total por tipo y las comisiones aparte', () => {
    responder(LISTA_MIXTA);

    expect(filas('tfoot tr')).toEqual([
      'Total compras | $112.00',
      'Total ventas | $78.00',
      'Total comisiones | $0.30',
    ]);
    // La comisión total va en la columna Comisión (5.ª) y no en Monto.
    expect(elemento.querySelectorAll('tfoot tr')[2].children[1].textContent?.trim()).toBe('$0.30');
    expect(filas('.tarjeta-total')).toEqual([
      'Total compras | $112.00',
      'Total ventas | $78.00',
      'Total comisiones | $0.30',
    ]);
  });

  it('muestra los conteos del backend en las pestañas', () => {
    responder(LISTA_MIXTA);

    expect(pestanas().map((p) => p.textContent?.replace(/\s+/g, ' ').trim())).toEqual([
      'Todas 2',
      'Compras 1',
      'Ventas 1',
    ]);
    expect(pestanas()[0].getAttribute('aria-selected')).toBe('true');
  });

  it('filtra por tipo usando la API al elegir una pestaña', () => {
    responder(LISTA_MIXTA);

    pestanas()[2].click();
    fixture.detectChanges();
    const solicitud = consulta();
    expect(solicitud.request.params.get('tipo')).toBe('VENTA');
    expect(solicitud.request.params.has('activoId')).toBe(false);
    solicitud.flush({ ...LISTA_MIXTA, transacciones: [LISTA_MIXTA.transacciones[0]], totalComprado: 0, totalComisiones: 0.15 });
    fixture.detectChanges();

    expect(pestanas()[2].getAttribute('aria-selected')).toBe('true');
    expect(filas('tfoot tr')).toEqual(['Total ventas | $0.15 | $78.00']);
  });

  it('mueve y activa la pestaña con las flechas del teclado', () => {
    responder(LISTA_MIXTA);

    pestanas()[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowLeft' }));
    fixture.detectChanges();
    expect(consulta().request.params.get('tipo')).toBe('VENTA');
  });

  it('ofrece todos los activos en el selector y filtra por activo usando la API', () => {
    responder(LISTA_MIXTA);

    expect(textos('.selector-activo option')).toEqual(['Todos los activos', 'BTC', 'IVV']);
    const selector = elemento.querySelector<HTMLSelectElement>('.selector-activo')!;
    selector.value = '1';
    selector.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const solicitud = consulta();
    expect(solicitud.request.params.get('activoId')).toBe('1');
    expect(solicitud.request.params.has('tipo')).toBe(false);
    solicitud.flush(LISTA_MIXTA);
  });

  it('con filtros sin resultados mantiene los filtros y lo indica', () => {
    responder(LISTA_COMPRAS);

    pestanas()[2].click();
    fixture.detectChanges();
    consulta().flush({ ...LISTA_VACIA, conteoCompras: 7 });
    fixture.detectChanges();

    expect(texto('.sin-resultados')).toBe('No hay ventas con este filtro.');
    expect(elemento.querySelector('[role="tablist"]')).not.toBeNull();
    expect(elemento.querySelector('.vacio')).toBeNull();
  });

  it('si falla una consulta filtrada mantiene los filtros y permite reintentar con ellos', () => {
    responder(LISTA_MIXTA);

    pestanas()[1].click();
    fixture.detectChanges();
    consulta().flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(elemento.querySelector('[role="tablist"]')).not.toBeNull();
    elemento.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    fixture.detectChanges();
    const reintento = consulta();
    expect(reintento.request.params.get('tipo')).toBe('COMPRA');
    reintento.flush(LISTA_COMPRAS);
  });

  it('una respuesta anterior no reemplaza a la de un filtro más reciente', () => {
    responder(LISTA_MIXTA);

    pestanas()[1].click();
    fixture.detectChanges();
    const anterior = consulta();
    pestanas()[2].click();
    fixture.detectChanges();

    expect(anterior.cancelled).toBe(true);
    consulta().flush({ ...LISTA_MIXTA, transacciones: [LISTA_MIXTA.transacciones[0]] });
    fixture.detectChanges();
    expect(elemento.querySelectorAll('tbody tr').length).toBe(1);
  });
});
