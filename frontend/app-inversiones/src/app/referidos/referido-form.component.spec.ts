import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Activo } from '../activos/activo.model';
import { ReferidoFormComponent } from './referido-form.component';

const ACTIVOS: Activo[] = [
  { id: 3, simbolo: 'BTC', nombre: 'Bitcoin', tipoActivo: 'CRIPTOMONEDA', moneda: 'USD', simboloEditable: true },
  { id: 1, simbolo: 'IVV', nombre: 'iShares Core S&P 500 ETF', tipoActivo: 'ETF', moneda: 'USD', simboloEditable: true },
];

describe('ReferidoFormComponent', () => {
  let fixture: ComponentFixture<ReferidoFormComponent>;
  let http: HttpTestingController;
  let elemento: HTMLElement;
  let guardados: number;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReferidoFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ReferidoFormComponent);
    guardados = 0;
    fixture.componentInstance.guardado.subscribe(() => guardados++);
    elemento = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function responderActivos(): void {
    http.expectOne('/api/v1/activos').flush(ACTIVOS);
    fixture.detectChanges();
  }

  function escribir(selector: string, valor: string): void {
    const control = elemento.querySelector<HTMLInputElement | HTMLSelectElement>(selector)!;
    control.value = valor;
    control.dispatchEvent(new Event(control instanceof HTMLSelectElement ? 'change' : 'input'));
    fixture.detectChanges();
  }

  function texto(selector: string): string {
    return elemento.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  }

  function enviar(): void {
    elemento.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    fixture.detectChanges();
  }

  function fecha(): string {
    return elemento.querySelector<HTMLInputElement>('#referido-fecha')!.value;
  }

  function completarReferidoExcel(): void {
    escribir('#referido-activo', '3');
    escribir('#referido-origen', 'Programa de referidos Hapi');
    escribir('#referido-cantidad', '0.00001196');
    escribir('#referido-precio', '83580.27');
  }

  it('empieza con la fecha de hoy y ofrece todos los activos con símbolo y nombre', () => {
    expect(texto('#referido-activo option')).toBe('Cargando activos…');
    responderActivos();

    expect(fecha()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    const opciones = Array.from(elemento.querySelectorAll('#referido-activo option')).map((o) => o.textContent?.trim());
    expect(opciones).toEqual(['Elige un activo', 'BTC · Bitcoin', 'IVV · iShares Core S&P 500 ETF']);
    expect(texto('button[type="submit"]')).toBe('Registrar referido');
  });

  it('indica si no puede cargar los activos y permite reintentar', () => {
    http.expectOne('/api/v1/activos').flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(texto('.activos-error')).toBe('No fue posible cargar los activos. Reintentar');
    elemento.querySelector<HTMLButtonElement>('.activos-error button')!.click();
    responderActivos();
    expect(elemento.querySelector('.activos-error')).toBeNull();
    expect(elemento.querySelectorAll('#referido-activo option').length).toBe(3);
  });

  it('muestra la vista previa no editable del Excel: 0.00001196 × 83580.27 = $1.00', () => {
    responderActivos();
    escribir('#referido-cantidad', '0.00001196');
    escribir('#referido-precio', '83580.27');

    expect(texto('.calculado-titulo')).toBe('Valor recibido');
    expect(texto('.calculado-valor')).toBe('$1.00');
    expect(texto('.calculado-nota')).toBe('Calculado · 0.00001196 × $83,580.27');
    expect(elemento.querySelector('.calculado input, .calculado select, .calculado textarea')).toBeNull();
  });

  it('redondea la vista previa a 2 decimales mitad hacia arriba y puede dar $0.00', () => {
    responderActivos();
    escribir('#referido-cantidad', '0.00000001');
    escribir('#referido-precio', '100');
    expect(texto('.calculado-valor')).toBe('$0.00');

    escribir('#referido-cantidad', '0.5');
    escribir('#referido-precio', '0.01');
    expect(texto('.calculado-valor')).toBe('$0.01');
  });

  it('no muestra vista previa mientras cantidad o precio no son válidos', () => {
    responderActivos();
    escribir('#referido-cantidad', 'abc');
    escribir('#referido-precio', '100');
    expect(texto('.calculado-valor')).toBe('—');
    expect(texto('.calculado-nota')).toBe('Calculado');
  });

  it('valida en el cliente antes de enviar y no llama a la API', () => {
    responderActivos();
    escribir('#referido-origen', '   ');
    escribir('#referido-cantidad', '0');
    escribir('#referido-precio', '1.1234567');
    enviar();

    expect(texto('#referido-activo-error')).toBe('⚠Es obligatorio.');
    expect(texto('#referido-origen-error')).toBe('⚠Es obligatorio.');
    expect(texto('#referido-cantidad-error')).toBe('⚠Debe ser mayor que cero.');
    expect(texto('#referido-precio-error')).toBe('⚠Admite como máximo 6 decimales y 13 dígitos enteros.');
    http.expectNone('/api/v1/referidos');
  });

  it('rechaza un origen de más de 100 caracteres y una cantidad con más de 8 decimales', () => {
    responderActivos();
    escribir('#referido-origen', 'x'.repeat(101));
    escribir('#referido-cantidad', '0.000000001');
    enviar();

    expect(texto('#referido-origen-error')).toBe('⚠Admite como máximo 100 caracteres.');
    expect(texto('#referido-cantidad-error')).toBe('⚠Admite como máximo 8 decimales y 16 dígitos enteros.');
  });

  it('registra el referido sin enviar el valor recibido y avisa a la pantalla', () => {
    responderActivos();
    completarReferidoExcel();
    escribir('#referido-comentario', '  Bono  ');
    enviar();

    const solicitud = http.expectOne('/api/v1/referidos');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual({
      activoId: 3,
      fechaReferido: fecha(),
      origen: 'Programa de referidos Hapi',
      cantidad: 0.00001196,
      precioAlRecibir: 83580.27,
      comentario: 'Bono',
    });
    expect(texto('button[type="submit"]')).toBe('Guardando…');
    solicitud.flush({}, { status: 201, statusText: 'Created' });

    expect(guardados).toBe(1);
  });

  it('envía el comentario vacío como null', () => {
    responderActivos();
    completarReferidoExcel();
    escribir('#referido-comentario', '   ');
    enviar();

    const solicitud = http.expectOne('/api/v1/referidos');
    expect(solicitud.request.body.comentario).toBeNull();
    solicitud.flush({}, { status: 201, statusText: 'Created' });
  });

  it('muestra el 400 bajo su campo sin perder lo ingresado', () => {
    responderActivos();
    completarReferidoExcel();
    escribir('#referido-fecha', '2099-01-01');
    enviar();

    http.expectOne('/api/v1/referidos').flush(
      {
        estadoHttp: 400,
        codigo: 'DATOS_INVALIDOS',
        mensaje: 'La solicitud contiene datos inválidos.',
        erroresCampos: [{ campo: 'fechaReferido', mensaje: 'No puede ser una fecha futura.' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    expect(texto('#referido-fecha-error')).toBe('⚠No puede ser una fecha futura.');
    expect(fecha()).toBe('2099-01-01');
    expect(elemento.querySelector<HTMLInputElement>('#referido-origen')!.value).toBe('Programa de referidos Hapi');
    expect(elemento.querySelector<HTMLInputElement>('#referido-cantidad')!.value).toBe('0.00001196');
    expect(elemento.querySelector<HTMLInputElement>('#referido-precio')!.value).toBe('83580.27');
    expect(guardados).toBe(0);
  });

  it('muestra un error general comprensible si el servidor falla', () => {
    responderActivos();
    completarReferidoExcel();
    enviar();
    http.expectOne('/api/v1/referidos').flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(texto('.error-general')).toBe(
      '⚠No fue posible registrar el referido. Inténtalo de nuevo en unos momentos.',
    );
    expect(texto('button[type="submit"]')).toBe('Registrar referido');
  });
});
