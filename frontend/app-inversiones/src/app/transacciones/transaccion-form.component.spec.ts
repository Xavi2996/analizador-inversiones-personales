import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Activo } from '../activos/activo.model';
import { TipoTransaccion } from './transaccion.model';
import { TransaccionFormComponent } from './transaccion-form.component';

const ACTIVOS: Activo[] = [
  { id: 3, simbolo: 'BTC', nombre: 'Bitcoin', tipoActivo: 'CRIPTOMONEDA', moneda: 'USD', simboloEditable: true },
  { id: 1, simbolo: 'IVV', nombre: 'iShares Core S&P 500 ETF', tipoActivo: 'ETF', moneda: 'USD', simboloEditable: true },
];

describe('TransaccionFormComponent', () => {
  let fixture: ComponentFixture<TransaccionFormComponent>;
  let http: HttpTestingController;
  let elemento: HTMLElement;
  let guardados: TipoTransaccion[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TransaccionFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TransaccionFormComponent);
    fixture.componentRef.setInput('activos', ACTIVOS);
    guardados = [];
    fixture.componentInstance.guardado.subscribe((tipo) => guardados.push(tipo));
    elemento = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function escribir(selector: string, valor: string): void {
    const control = elemento.querySelector<HTMLInputElement | HTMLSelectElement>(selector)!;
    control.value = valor;
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input'));
    fixture.detectChanges();
  }

  function texto(selector: string): string {
    return elemento.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  }

  function elegirVenta(): void {
    elemento.querySelectorAll<HTMLButtonElement>('[role="radio"]')[1].click();
    fixture.detectChanges();
  }

  function enviar(): void {
    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
  }

  function fecha(): string {
    return elemento.querySelector<HTMLInputElement>('#transaccion-fecha')!.value;
  }

  function consultaDisponible(): TestRequest {
    return http.expectOne((r) => r.url === '/api/v1/transacciones/disponible');
  }

  function completarCompra(): void {
    escribir('#transaccion-activo', '1');
    escribir('#transaccion-cantidad', '0.14553');
    escribir('#transaccion-precio', '769.57');
    escribir('#transaccion-comision', '0.15');
  }

  it('empieza en Compra con la fecha de hoy y sin consultar la cantidad disponible', () => {
    expect(texto('[role="radio"][aria-checked="true"]')).toBe('+Compra');
    expect(fecha()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(texto('button[type="submit"]')).toBe('Registrar compra');
    expect(elemento.querySelector('.disponible')).toBeNull();
  });

  it('ofrece todos los activos con símbolo y nombre', () => {
    const opciones = Array.from(elemento.querySelectorAll('#transaccion-activo option')).map((o) => o.textContent?.trim());
    expect(opciones).toEqual(['Elige un activo', 'BTC · Bitcoin', 'IVV · iShares Core S&P 500 ETF']);
  });

  it('muestra la vista previa no editable: 0.10 × 770.00 = $77.00 y el efectivo utilizado', () => {
    escribir('#transaccion-cantidad', '0.10');
    escribir('#transaccion-precio', '770.00');
    escribir('#transaccion-comision', '0.15');

    expect(texto('.calculado-valor')).toBe('$77.00');
    expect(texto('.calculado-nota')).toBe('Calculado · 0.10 × $770.00');
    expect(texto('.calculado-efectivo')).toBe('Efectivo utilizado · monto + comisión $77.15');
    expect(elemento.querySelector('.calculado input, .calculado select, .calculado textarea')).toBeNull();
  });

  it('redondea la vista previa a 2 decimales mitad hacia arriba, igual que el backend', () => {
    escribir('#transaccion-cantidad', '0.14553');
    escribir('#transaccion-precio', '769.57');
    expect(texto('.calculado-valor')).toBe('$112.00');

    escribir('#transaccion-cantidad', '0.00020734');
    escribir('#transaccion-precio', '81990.93');
    expect(texto('.calculado-valor')).toBe('$17.00');
  });

  it('no muestra vista previa mientras cantidad o precio no son válidos', () => {
    escribir('#transaccion-cantidad', 'abc');
    escribir('#transaccion-precio', '770');
    expect(texto('.calculado-valor')).toBe('—');
  });

  it('en Venta muestra el efectivo recibido y el botón Registrar venta', () => {
    elegirVenta();
    escribir('#transaccion-cantidad', '0.10');
    escribir('#transaccion-precio', '770.00');
    escribir('#transaccion-comision', '0.15');

    expect(texto('.calculado-efectivo')).toBe('Efectivo recibido · monto − comisión $76.85');
    expect(texto('button[type="submit"]')).toBe('Registrar venta');
  });

  it('en Venta consulta la cantidad disponible al backend y la recarga al cambiar activo o fecha', () => {
    elegirVenta();
    expect(texto('.disponible')).toContain('Elige el activo y la fecha');

    escribir('#transaccion-activo', '1');
    let solicitud = consultaDisponible();
    expect(solicitud.request.params.get('activoId')).toBe('1');
    expect(solicitud.request.params.get('fecha')).toBe(fecha());
    expect(texto('.disponible')).toBe('Consultando la cantidad disponible…');
    solicitud.flush({ activoId: 1, fecha: fecha(), cantidadDisponible: 0.54308 });
    fixture.detectChanges();
    expect(texto('.disponible')).toBe('Disponible para vender: 0.54308000 IVV');

    escribir('#transaccion-fecha', '2026-10-19');
    solicitud = consultaDisponible();
    expect(solicitud.request.params.get('fecha')).toBe('2026-10-19');
    solicitud.flush({ activoId: 1, fecha: '2026-10-19', cantidadDisponible: 0.39755 });
    fixture.detectChanges();
    expect(texto('.disponible')).toBe('Disponible para vender: 0.39755000 IVV');

    escribir('#transaccion-activo', '3');
    solicitud = consultaDisponible();
    expect(solicitud.request.params.get('activoId')).toBe('3');
    solicitud.flush({ activoId: 3, fecha: '2026-10-19', cantidadDisponible: 0.00020734 });
    fixture.detectChanges();
    expect(texto('.disponible')).toBe('Disponible para vender: 0.00020734 BTC');
  });

  it('cambiar cantidad o precio en Venta no vuelve a consultar el disponible', () => {
    elegirVenta();
    escribir('#transaccion-activo', '1');
    consultaDisponible().flush({ activoId: 1, fecha: fecha(), cantidadDisponible: 0.5 });
    escribir('#transaccion-cantidad', '0.1');
    escribir('#transaccion-precio', '770');
    http.expectNone((r) => r.url === '/api/v1/transacciones/disponible');
  });

  it('indica cuando no puede consultar la cantidad disponible', () => {
    elegirVenta();
    escribir('#transaccion-activo', '1');
    consultaDisponible().flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    expect(texto('.disponible')).toBe('No fue posible consultar la cantidad disponible.');
  });

  it('valida en el cliente antes de enviar y no llama a la API', () => {
    escribir('#transaccion-cantidad', '0');
    escribir('#transaccion-precio', '1.1234567');
    escribir('#transaccion-comision', '-1');
    enviar();

    expect(texto('#transaccion-activo-error')).toBe('⚠Es obligatorio.');
    expect(texto('#transaccion-cantidad-error')).toBe('⚠Debe ser mayor que cero.');
    expect(texto('#transaccion-precio-error')).toBe('⚠Admite como máximo 6 decimales y 13 dígitos enteros.');
    expect(texto('#transaccion-comision-error')).toBe('⚠No puede ser negativo.');
    http.expectNone('/api/v1/transacciones');
  });

  it('rechaza una cantidad con más de 8 decimales', () => {
    escribir('#transaccion-cantidad', '0.000000001');
    enviar();
    expect(texto('#transaccion-cantidad-error')).toBe('⚠Admite como máximo 8 decimales y 16 dígitos enteros.');
  });

  it('registra la compra sin enviar monto ni efectivo y avisa a la pantalla', () => {
    completarCompra();
    escribir('#transaccion-comentario', '  Compra IVV  ');
    enviar();

    const solicitud = http.expectOne('/api/v1/transacciones');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual({
      activoId: 1,
      tipoTransaccion: 'COMPRA',
      fechaTransaccion: fecha(),
      cantidad: 0.14553,
      precioUnitario: 769.57,
      comision: 0.15,
      comentario: 'Compra IVV',
    });
    expect(texto('button[type="submit"]')).toBe('Guardando…');
    solicitud.flush({}, { status: 201, statusText: 'Created' });

    expect(guardados).toEqual(['COMPRA']);
  });

  it('muestra el 409 bajo la cantidad sin perder lo ingresado y vuelve a consultar el disponible', () => {
    elegirVenta();
    completarCompra();
    consultaDisponible().flush({ activoId: 1, fecha: fecha(), cantidadDisponible: 0.1 });
    enviar();

    const mensaje = 'La cantidad supera la disponible para vender en esa fecha (0.10000000 IVV).';
    http.expectOne('/api/v1/transacciones').flush(
      {
        estadoHttp: 409,
        codigo: 'CANTIDAD_INSUFICIENTE',
        mensaje,
        erroresCampos: [{ campo: 'cantidad', mensaje }],
      },
      { status: 409, statusText: 'Conflict' },
    );
    fixture.detectChanges();

    expect(texto('#transaccion-cantidad-error')).toBe(`⚠${mensaje}`);
    expect(elemento.querySelector<HTMLInputElement>('#transaccion-cantidad')!.value).toBe('0.14553');
    expect(elemento.querySelector<HTMLInputElement>('#transaccion-precio')!.value).toBe('769.57');
    expect(guardados).toEqual([]);
    consultaDisponible().flush({ activoId: 1, fecha: fecha(), cantidadDisponible: 0.1 });
  });

  it('muestra bajo la comisión el 400 de comisión mayor que el monto de la venta', () => {
    elegirVenta();
    completarCompra();
    consultaDisponible().flush({ activoId: 1, fecha: fecha(), cantidadDisponible: 1 });
    escribir('#transaccion-comision', '200');
    enviar();

    http.expectOne('/api/v1/transacciones').flush(
      {
        estadoHttp: 400,
        codigo: 'DATOS_INVALIDOS',
        mensaje: 'La solicitud contiene datos inválidos.',
        erroresCampos: [{ campo: 'comision', mensaje: 'No puede ser mayor que el monto bruto de la venta.' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    expect(texto('#transaccion-comision-error')).toBe('⚠No puede ser mayor que el monto bruto de la venta.');
  });

  it('muestra un error general comprensible si el servidor falla', () => {
    completarCompra();
    enviar();
    http.expectOne('/api/v1/transacciones').flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(texto('.error-general')).toBe(
      '⚠No fue posible registrar la transacción. Inténtalo de nuevo en unos momentos.',
    );
    expect(texto('button[type="submit"]')).toBe('Registrar compra');
  });
});
