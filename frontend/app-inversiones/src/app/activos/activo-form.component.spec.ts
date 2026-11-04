import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivoFormComponent, PanelActivo, ResultadoGuardado } from './activo-form.component';
import { Activo } from './activo.model';

const IVV: Activo = {
  id: 1,
  simbolo: 'IVV',
  nombre: 'iShares Core S&P 500 ETF',
  tipoActivo: 'ETF',
  moneda: 'USD',
  simboloEditable: true,
};

describe('ActivoFormComponent', () => {
  let fixture: ComponentFixture<ActivoFormComponent>;
  let http: HttpTestingController;
  let elemento: HTMLElement;
  let resultado: ResultadoGuardado | undefined;
  let cerrado: boolean;

  async function crear(panel: PanelActivo, porcentajeActual: number | null = null) {
    await TestBed.configureTestingModule({
      imports: [ActivoFormComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ActivoFormComponent);
    fixture.componentRef.setInput('panel', panel);
    fixture.componentRef.setInput('porcentajeActual', porcentajeActual);
    resultado = undefined;
    cerrado = false;
    fixture.componentInstance.guardado.subscribe((r) => (resultado = r));
    fixture.componentInstance.cerrado.subscribe(() => (cerrado = true));
    elemento = fixture.nativeElement;
    fixture.detectChanges();
    await fixture.whenStable();
  }

  afterEach(() => http.verify());

  function escribir(selector: string, valor: string): void {
    const campo = elemento.querySelector<HTMLInputElement>(selector)!;
    campo.value = valor;
    campo.dispatchEvent(new Event('input'));
  }

  function elegirTipo(etiqueta: string): void {
    const opcion = Array.from(elemento.querySelectorAll<HTMLLabelElement>('.segmento')).find(
      (l) => l.textContent?.trim() === etiqueta,
    )!;
    opcion.querySelector('input')!.click();
  }

  function enviar(): void {
    elemento.querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function errores(): string[] {
    return Array.from(elemento.querySelectorAll('.campo-error')).map((e) => e.textContent!.replace('⚠', '').trim());
  }

  describe('al registrar', () => {
    beforeEach(() => crear({ modo: 'crear' }));

    it('muestra el título y los campos del formulario', () => {
      expect(elemento.querySelector('h2')?.textContent).toBe('Registrar activo');
      expect(elemento.querySelectorAll('.segmento').length).toBe(3);
      expect(elemento.querySelector<HTMLSelectElement>('#activo-moneda')?.value).toBe('USD');
    });

    it('valida los campos obligatorios sin llamar a la API', () => {
      enviar();

      expect(errores()).toEqual(['Es obligatorio.', 'Es obligatorio.', 'Es obligatorio.']);
      http.expectNone('/api/v1/activos');
    });

    it('valida el formato del símbolo y el rango del porcentaje', () => {
      escribir('#activo-simbolo', 'IV V');
      escribir('#activo-objetivo', '100.01');
      enviar();

      expect(errores()).toContain('Solo se permiten letras, números, punto y guion (máximo 20).');
      expect(errores()).toContain('Debe estar entre 0 y 100.');
    });

    it('registra el activo normalizado y después su porcentaje objetivo', () => {
      escribir('#activo-simbolo', ' ivv ');
      escribir('#activo-nombre', ' iShares Core S&P 500 ETF ');
      elegirTipo('ETF');
      escribir('#activo-objetivo', '85');
      enviar();

      const alta = http.expectOne('/api/v1/activos');
      expect(alta.request.method).toBe('POST');
      expect(alta.request.body).toEqual({
        simbolo: 'IVV',
        nombre: 'iShares Core S&P 500 ETF',
        tipoActivo: 'ETF',
        moneda: 'USD',
      });
      alta.flush(IVV, { status: 201, statusText: 'Created' });

      const objetivo = http.expectOne('/api/v1/asignaciones/1');
      expect(objetivo.request.method).toBe('PUT');
      expect(objetivo.request.body).toEqual({ porcentajeObjetivo: 85 });
      objetivo.flush({ activoId: 1, simbolo: 'IVV', porcentajeObjetivo: 85 });

      expect(resultado).toEqual({ tipo: 'exito', mensaje: 'Activo registrado correctamente.' });
    });

    it('no guarda asignación si el porcentaje está vacío', () => {
      escribir('#activo-simbolo', 'BTC');
      escribir('#activo-nombre', 'Bitcoin');
      elegirTipo('Criptomoneda');
      enviar();

      http.expectOne('/api/v1/activos').flush({ ...IVV, id: 3, simbolo: 'BTC', tipoActivo: 'CRIPTOMONEDA' });
      http.expectNone('/api/v1/asignaciones/3');
      expect(resultado?.tipo).toBe('exito');
    });

    it('muestra bajo el símbolo el error de símbolo duplicado de la API', () => {
      escribir('#activo-simbolo', 'IVV');
      escribir('#activo-nombre', 'Otro');
      elegirTipo('ETF');
      enviar();

      http.expectOne('/api/v1/activos').flush(
        {
          codigo: 'SIMBOLO_DUPLICADO',
          estadoHttp: 409,
          mensaje: 'Ya existe un activo con el símbolo IVV.',
          erroresCampos: [{ campo: 'simbolo', mensaje: 'Ya existe un activo con el símbolo IVV.' }],
        },
        { status: 409, statusText: 'Conflict' },
      );
      fixture.detectChanges();

      expect(elemento.querySelector('#activo-simbolo-error')?.textContent).toContain(
        'Ya existe un activo con el símbolo IVV.',
      );
      expect(elemento.querySelector('#activo-simbolo')?.getAttribute('aria-invalid')).toBe('true');
      expect(resultado).toBeUndefined();
    });

    it('muestra un error general comprensible si falla el servidor', () => {
      escribir('#activo-simbolo', 'IVV');
      escribir('#activo-nombre', 'iShares');
      elegirTipo('ETF');
      enviar();

      http.expectOne('/api/v1/activos').flush(null, { status: 500, statusText: 'Error' });
      fixture.detectChanges();

      expect(elemento.querySelector('.error-general')?.textContent).toContain('No fue posible guardar el activo.');
    });

    it('avisa si el activo se guardó pero el objetivo no', () => {
      escribir('#activo-simbolo', 'IVV');
      escribir('#activo-nombre', 'iShares');
      elegirTipo('ETF');
      escribir('#activo-objetivo', '85');
      enviar();

      http.expectOne('/api/v1/activos').flush(IVV);
      http.expectOne('/api/v1/asignaciones/1').flush(null, { status: 500, statusText: 'Error' });

      expect(resultado?.tipo).toBe('advertencia');
      expect(resultado?.mensaje).toContain('se guardó, pero no fue posible guardar su porcentaje objetivo');
    });

    it('emite cerrado al cancelar', () => {
      elemento.querySelector<HTMLButtonElement>('.cancelar')!.click();
      expect(cerrado).toBe(true);
    });
  });

  describe('al editar', () => {
    it('precarga los datos y guarda un símbolo nuevo con PUT', async () => {
      await crear({ modo: 'editar', activo: IVV }, 85);

      expect(elemento.querySelector('h2')?.textContent).toBe('Editar IVV');
      expect(elemento.querySelector<HTMLInputElement>('#activo-simbolo')?.value).toBe('IVV');
      expect(elemento.querySelector<HTMLInputElement>('#activo-objetivo')?.value).toBe('85');

      escribir('#activo-simbolo', 'ivv.x');
      enviar();

      const cambio = http.expectOne('/api/v1/activos/1');
      expect(cambio.request.method).toBe('PUT');
      expect(cambio.request.body.simbolo).toBe('IVV.X');
      cambio.flush({ ...IVV, simbolo: 'IVV.X' });
      http.expectOne('/api/v1/asignaciones/1').flush({ activoId: 1, simbolo: 'IVV.X', porcentajeObjetivo: 85 });

      expect(resultado).toEqual({ tipo: 'exito', mensaje: 'Cambios guardados correctamente.' });
    });

    it('muestra el símbolo como "No editable" si el backend lo bloquea', async () => {
      await crear({ modo: 'editar', activo: { ...IVV, simboloEditable: false } });

      expect(elemento.querySelector('#activo-simbolo')).toBeNull();
      expect(elemento.querySelector('.simbolo-fijo')?.textContent).toContain('No editable');

      enviar();
      const cambio = http.expectOne('/api/v1/activos/1');
      expect(cambio.request.body.simbolo).toBe('IVV');
      cambio.flush(IVV);
    });

    it('no permite vaciar un objetivo ya registrado', async () => {
      await crear({ modo: 'editar', activo: IVV }, 85);

      escribir('#activo-objetivo', '');
      enviar();

      expect(errores()).toContain('No se puede dejar vacío. Para excluir el activo del objetivo, indica 0.');
      http.expectNone('/api/v1/activos/1');
    });
  });
});
