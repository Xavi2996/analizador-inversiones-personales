import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AporteFormComponent } from './aporte-form.component';
import { Aporte } from './aporte.model';

const REGISTRADO: Aporte = {
  id: 3,
  fechaAporte: '2026-10-21',
  montoAportado: 200.0,
  montoNetoBroker: 195.58,
  costoFondeo: 4.42,
  porcentajeCostoFondeo: 2.21,
  metodoFondeo: 'Binance USDT → ERC20 → Hapi',
  comentario: null,
};

describe('AporteFormComponent', () => {
  let fixture: ComponentFixture<AporteFormComponent>;
  let http: HttpTestingController;
  let elemento: HTMLElement;
  let guardado: boolean;
  let cerrado: boolean;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AporteFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AporteFormComponent);
    guardado = false;
    cerrado = false;
    fixture.componentInstance.guardado.subscribe(() => (guardado = true));
    fixture.componentInstance.cerrado.subscribe(() => (cerrado = true));
    elemento = fixture.nativeElement;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function campo(id: string): HTMLInputElement {
    return elemento.querySelector<HTMLInputElement>(`#${id}`)!;
  }

  function escribir(id: string, valor: string): void {
    const control = campo(id);
    control.value = valor;
    control.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function enviar(): void {
    elemento.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function texto(selector: string): string {
    return elemento.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  }

  function llenarValido(): void {
    escribir('aporte-fecha', '2026-10-21');
    escribir('aporte-monto', '200.00');
    escribir('aporte-neto', '195.58');
  }

  it('propone la fecha de hoy del navegador', () => {
    const ahora = new Date();
    const esperado = [
      ahora.getFullYear(),
      String(ahora.getMonth() + 1).padStart(2, '0'),
      String(ahora.getDate()).padStart(2, '0'),
    ].join('-');
    expect(campo('aporte-fecha').value).toBe(esperado);
    expect(campo('aporte-fecha').type).toBe('date');
  });

  it('muestra la vista previa del costo de fondeo, que no es un campo editable', () => {
    expect(texto('.calculado-valor')).toBe('—');

    escribir('aporte-monto', '200.00');
    escribir('aporte-neto', '195.58');

    expect(texto('.calculado-valor')).toBe('$4.42');
    expect(texto('.calculado-nota')).toBe('Calculado · $200.00 − $195.58');
    expect(elemento.querySelector('.calculado input, .calculado textarea')).toBeNull();
    expect(elemento.querySelector('[formcontrolname="costoFondeo"]')).toBeNull();
  });

  it('no muestra costo previo si el neto supera el aportado o un monto no es válido', () => {
    escribir('aporte-monto', '100');
    escribir('aporte-neto', '100.01');
    expect(texto('.calculado-valor')).toBe('—');

    escribir('aporte-neto', 'abc');
    expect(texto('.calculado-valor')).toBe('—');
  });

  it('valida los campos antes de enviar sin llamar a la API', () => {
    escribir('aporte-fecha', '');
    escribir('aporte-monto', '0');
    escribir('aporte-neto', '-1');
    escribir('aporte-metodo', 'M'.repeat(51));
    escribir('aporte-comentario', 'C'.repeat(501));
    enviar();

    expect(texto('#aporte-fecha-error')).toBe('⚠Es obligatorio.');
    expect(texto('#aporte-monto-error')).toBe('⚠Debe ser mayor que cero.');
    expect(texto('#aporte-neto-error')).toBe('⚠No puede ser negativo.');
    expect(texto('#aporte-metodo-error')).toBe('⚠Admite como máximo 50 caracteres.');
    expect(texto('#aporte-comentario-error')).toBe('⚠Admite como máximo 500 caracteres.');
    expect(campo('aporte-monto').getAttribute('aria-invalid')).toBe('true');
    http.expectNone('/api/v1/aportes');
  });

  it('rechaza montos con más de 2 decimales o que no son números', () => {
    escribir('aporte-monto', '100.001');
    escribir('aporte-neto', '1e3');
    enviar();

    expect(texto('#aporte-monto-error')).toBe('⚠Admite como máximo 2 decimales y 13 dígitos enteros.');
    expect(texto('#aporte-neto-error')).toBe('⚠Debe ser un número.');
    http.expectNone('/api/v1/aportes');
  });

  it('registra el aporte sin enviar el costo y con textos opcionales normalizados', () => {
    llenarValido();
    escribir('aporte-metodo', '  Binance USDT → ERC20 → Hapi  ');
    escribir('aporte-comentario', '   ');
    enviar();

    const solicitud = http.expectOne('/api/v1/aportes');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual({
      fechaAporte: '2026-10-21',
      montoAportado: 200,
      montoNetoBroker: 195.58,
      metodoFondeo: 'Binance USDT → ERC20 → Hapi',
      comentario: null,
    });
    expect(texto('button[type="submit"]')).toBe('Guardando…');

    solicitud.flush(REGISTRADO, { status: 201, statusText: 'Created' });
    fixture.detectChanges();

    expect(guardado).toBe(true);
    expect(campo('aporte-monto').value).toBe('');
    expect(campo('aporte-neto').value).toBe('');
    expect(texto('.calculado-valor')).toBe('—');
  });

  it('muestra bajo "Monto neto recibido" el mensaje de la API cuando el neto supera el aportado', async () => {
    escribir('aporte-fecha', '2026-10-21');
    escribir('aporte-monto', '200.00');
    escribir('aporte-neto', '200.01');
    enviar();

    http.expectOne('/api/v1/aportes').flush(
      {
        fechaHora: '2026-10-21T10:00:00-05:00',
        estadoHttp: 400,
        codigo: 'DATOS_INVALIDOS',
        mensaje: 'La solicitud contiene datos inválidos.',
        ruta: '/api/v1/aportes',
        erroresCampos: [{ campo: 'montoNetoBroker', mensaje: 'No puede ser mayor al monto aportado.' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();
    await fixture.whenStable();

    expect(texto('#aporte-neto-error')).toBe('⚠No puede ser mayor al monto aportado.');
    expect(campo('aporte-neto').getAttribute('aria-invalid')).toBe('true');
    expect(guardado).toBe(false);
    expect(texto('button[type="submit"]')).toBe('Registrar aporte');
  });

  it('muestra el error de fecha futura que devuelve la API bajo la fecha', () => {
    llenarValido();
    enviar();

    http.expectOne('/api/v1/aportes').flush(
      {
        estadoHttp: 400,
        codigo: 'DATOS_INVALIDOS',
        mensaje: 'La solicitud contiene datos inválidos.',
        erroresCampos: [{ campo: 'fechaAporte', mensaje: 'No puede ser una fecha futura.' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    fixture.detectChanges();

    expect(texto('#aporte-fecha-error')).toBe('⚠No puede ser una fecha futura.');
  });

  it('muestra un error comprensible si el servidor falla', () => {
    llenarValido();
    enviar();

    http.expectOne('/api/v1/aportes').flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(texto('.error-general')).toBe(
      '⚠No fue posible registrar el aporte. Inténtalo de nuevo en unos momentos.',
    );
    expect(guardado).toBe(false);
  });

  it('se cierra con Cancelar', () => {
    elemento.querySelector<HTMLButtonElement>('.cancelar')!.click();
    expect(cerrado).toBe(true);
  });
});
