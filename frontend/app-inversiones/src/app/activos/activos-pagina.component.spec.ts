import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Activo, ListaAsignaciones } from './activo.model';
import { ActivosPaginaComponent } from './activos-pagina.component';

const ACTIVOS: Activo[] = [
  { id: 3, simbolo: 'BTC', nombre: 'Bitcoin', tipoActivo: 'CRIPTOMONEDA', moneda: 'USD', simboloEditable: true },
  { id: 1, simbolo: 'IVV', nombre: 'iShares Core S&P 500 ETF', tipoActivo: 'ETF', moneda: 'USD', simboloEditable: true },
  { id: 2, simbolo: 'VXUS', nombre: 'Vanguard Total International Stock ETF', tipoActivo: 'ETF', moneda: 'USD', simboloEditable: true },
];

const ASIGNACIONES_CORRECTAS: ListaAsignaciones = {
  asignaciones: [
    { activoId: 3, simbolo: 'BTC', porcentajeObjetivo: 5 },
    { activoId: 1, simbolo: 'IVV', porcentajeObjetivo: 85 },
    { activoId: 2, simbolo: 'VXUS', porcentajeObjetivo: 10 },
  ],
  sumaPorcentajes: 100,
  sumaCorrecta: true,
};

describe('ActivosPaginaComponent', () => {
  let fixture: ComponentFixture<ActivosPaginaComponent>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ActivosPaginaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ActivosPaginaComponent);
    elemento = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function responder(activos: Activo[], asignaciones: ListaAsignaciones): void {
    http.expectOne('/api/v1/activos').flush(activos);
    http.expectOne('/api/v1/asignaciones').flush(asignaciones);
    fixture.detectChanges();
  }

  function texto(selector: string): string {
    return elemento.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim() ?? '';
  }

  it('muestra el estado de carga mientras consulta la API', () => {
    expect(texto('[role="status"]')).toBe('Cargando activos…');
    responder(ACTIVOS, ASIGNACIONES_CORRECTAS);
  });

  it('muestra el estado vacío con la acción de registrar el primer activo', () => {
    responder([], { asignaciones: [], sumaPorcentajes: 0, sumaCorrecta: false });

    expect(texto('.vacio p')).toBe('No existen activos registrados.');
    expect(texto('.vacio button')).toBe('Registrar primer activo');
    expect(elemento.querySelector('table')).toBeNull();
  });

  it('lista los activos con tipo, moneda y objetivo', () => {
    responder(ACTIVOS, ASIGNACIONES_CORRECTAS);

    const filas = elemento.querySelectorAll('tbody tr');
    expect(filas.length).toBe(3);
    const ivv = filas[1].textContent!.replace(/\s+/g, ' ');
    expect(ivv).toContain('IVV');
    expect(ivv).toContain('iShares Core S&P 500 ETF');
    expect(ivv).toContain('ETF');
    expect(ivv).toContain('USD');
    expect(ivv).toContain('85.00%');
    expect(filas[0].textContent).toContain('Criptomoneda');
    expect(elemento.querySelectorAll('.tarjetas li').length).toBe(3);
  });

  it('no muestra la columna Estado ni las acciones Activar o Desactivar', () => {
    responder(ACTIVOS, ASIGNACIONES_CORRECTAS);

    const encabezados = Array.from(elemento.querySelectorAll('th')).map((th) => th.textContent?.trim());
    expect(encabezados).toEqual(['Activo', 'Tipo', 'Moneda', 'Objetivo', 'Acciones']);
    expect(elemento.textContent).not.toMatch(/Activar|Desactivar|INACTIVO/);
    expect(elemento.querySelector('button[aria-label="Editar IVV"]')).not.toBeNull();
  });

  it('muestra "—" en el objetivo de un activo sin asignación', () => {
    responder(ACTIVOS, {
      asignaciones: [{ activoId: 1, simbolo: 'IVV', porcentajeObjetivo: 85 }],
      sumaPorcentajes: 85,
      sumaCorrecta: false,
    });

    const btc = elemento.querySelectorAll('tbody tr')[0];
    expect(btc.querySelector('.numero')?.textContent?.trim()).toBe('—');
  });

  it('indica la suma correcta con símbolo y texto', () => {
    responder(ACTIVOS, ASIGNACIONES_CORRECTAS);

    expect(texto('.suma-valor')).toBe('100.00%');
    expect(texto('.suma-estado')).toBe('✓ suma correcta');
  });

  it('advierte con símbolo y texto cuando la suma no es 100%', () => {
    responder(ACTIVOS, {
      asignaciones: [
        { activoId: 1, simbolo: 'IVV', porcentajeObjetivo: 85 },
        { activoId: 2, simbolo: 'VXUS', porcentajeObjetivo: 10 },
      ],
      sumaPorcentajes: 95,
      sumaCorrecta: false,
    });

    expect(texto('.suma-valor')).toBe('95.00%');
    expect(texto('.suma-estado')).toBe('⚠ la suma debería ser 100%');
    expect(elemento.querySelector('.barra .sin-asignar')).not.toBeNull();
  });

  it('muestra un error comprensible y permite reintentar', () => {
    http.expectOne('/api/v1/activos').flush(null, { status: 500, statusText: 'Error' });
    // Si una consulta falla, forkJoin cancela la otra.
    expect(http.expectOne('/api/v1/asignaciones').cancelled).toBe(true);
    fixture.detectChanges();

    expect(texto('[role="alert"] p')).toContain('No fue posible cargar los activos.');

    elemento.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    fixture.detectChanges();
    expect(texto('[role="status"]')).toBe('Cargando activos…');
    responder(ACTIVOS, ASIGNACIONES_CORRECTAS);
    expect(elemento.querySelectorAll('tbody tr').length).toBe(3);
  });
});
