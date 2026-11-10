import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ListaAportes } from './aporte.model';
import { AportesPaginaComponent } from './aportes-pagina.component';

/** Datos de la hoja Aportes del Excel, en el orden que devuelve el backend. */
const LISTA_EXCEL: ListaAportes = {
  aportes: [
    {
      id: 3,
      fechaAporte: '2026-10-21',
      montoAportado: 200.0,
      montoNetoBroker: 195.58,
      costoFondeo: 4.42,
      porcentajeCostoFondeo: 2.21,
      metodoFondeo: 'Binance USDT → ERC20 → Hapi',
      comentario: 'Tercer aporte',
    },
    {
      id: 2,
      fechaAporte: '2026-10-19',
      montoAportado: 250.0,
      montoNetoBroker: 243.33,
      costoFondeo: 6.67,
      porcentajeCostoFondeo: 2.67,
      metodoFondeo: null,
      comentario: null,
    },
    {
      id: 1,
      fechaAporte: '2026-10-13',
      montoAportado: 125.0,
      montoNetoBroker: 120.22,
      costoFondeo: 4.78,
      porcentajeCostoFondeo: 3.82,
      metodoFondeo: 'Binance',
      comentario: null,
    },
  ],
  totalAportado: 575.0,
  totalNetoBroker: 559.13,
  totalCostoFondeo: 15.87,
  porcentajeCostoFondeo: 2.76,
  porcentajeNetoBroker: 97.24,
};

const LISTA_VACIA: ListaAportes = {
  aportes: [],
  totalAportado: 0,
  totalNetoBroker: 0,
  totalCostoFondeo: 0,
  porcentajeCostoFondeo: 0,
  porcentajeNetoBroker: 0,
};

describe('AportesPaginaComponent', () => {
  let fixture: ComponentFixture<AportesPaginaComponent>;
  let http: HttpTestingController;
  let elemento: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AportesPaginaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AportesPaginaComponent);
    elemento = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function responder(lista: ListaAportes): void {
    http.expectOne('/api/v1/aportes').flush(lista);
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
    expect(texto('[role="status"]')).toBe('Cargando aportes…');
    responder(LISTA_EXCEL);
  });

  it('muestra el estado vacío con la acción de registrar el primer aporte', () => {
    responder(LISTA_VACIA);

    expect(texto('.vacio p')).toBe('No existen aportes registrados.');
    expect(texto('.vacio button')).toBe('Registrar primer aporte');
    expect(elemento.querySelector('table')).toBeNull();
    expect(elemento.querySelector('.totales')).toBeNull();
  });

  it('muestra un error comprensible y permite reintentar', () => {
    http.expectOne('/api/v1/aportes').flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(texto('[role="alert"] p')).toContain('No fue posible cargar los aportes.');
    elemento.querySelector<HTMLButtonElement>('[role="alert"] button')!.click();
    fixture.detectChanges();

    expect(texto('[role="status"]')).toBe('Cargando aportes…');
    responder(LISTA_EXCEL);
    expect(elemento.querySelectorAll('tbody tr').length).toBe(3);
  });

  it('muestra los totales y los porcentajes del backend con texto junto a la barra', () => {
    responder(LISTA_EXCEL);

    const etiquetas = textos('.cifras .etiqueta');
    expect(etiquetas).toEqual(['Aportado desde bolsillo', 'Neto recibido', 'Costos de fondeo']);
    expect(textos('.cifras .cifra')).toEqual(['$575.00', '$559.13', '$15.87']);
    expect(textos('.barra-texto span')).toEqual(['97.24% llegó al intermediario', '2.76% en costos']);
  });

  it('usa los totales del backend sin sumar la lista', () => {
    // Totales deliberadamente distintos de la suma de la lista: deben mostrarse tal cual.
    responder({ ...LISTA_EXCEL, totalAportado: 999.99, porcentajeCostoFondeo: 1.23 });

    expect(texto('.cifra-principal')).toBe('$999.99');
    expect(texto('tfoot')).toContain('$999.99');
    expect(texto('tfoot')).toContain('1.23%');
  });

  it('lista el historial en el orden del backend con fechas DD/MM/AAAA y montos en USD', () => {
    responder(LISTA_EXCEL);

    const encabezados = Array.from(elemento.querySelectorAll('thead th')).map((th) => th.textContent?.trim());
    expect(encabezados).toEqual(['Fecha', 'Aportado', 'Neto recibido', 'Costo de fondeo', 'Método y comentario']);

    const filas = elemento.querySelectorAll('tbody tr');
    expect(filas.length).toBe(3);
    const primera = filas[0].textContent!.replace(/\s+/g, ' ');
    expect(primera).toContain('21/10/2026');
    expect(primera).toContain('$200.00');
    expect(primera).toContain('$195.58');
    expect(primera).toContain('$4.42 2.21%');
    expect(primera).toContain('Binance USDT → ERC20 → Hapi');
    expect(primera).toContain('Tercer aporte');
    expect(filas[2].textContent).toContain('13/10/2026');
    expect(filas[1].textContent).toContain('—');

    expect(textos('tfoot th, tfoot td')).toEqual(['Total', '$575.00', '$559.13', '$15.87 2.76%', '']);
    expect(texto('.historial-nota')).toBe('3 aportes · más reciente primero');
  });

  it('separa los miles en los montos', () => {
    responder({ ...LISTA_EXCEL, totalAportado: 12540.75 });

    expect(texto('.cifra-principal')).toBe('$12,540.75');
  });

  it('incluye la lista compacta para móvil con el total', () => {
    responder(LISTA_EXCEL);

    const tarjetas = elemento.querySelectorAll('.tarjetas li:not(.tarjeta-total)');
    expect(tarjetas.length).toBe(3);
    const primera = tarjetas[0].textContent!.replace(/\s+/g, ' ');
    expect(primera).toContain('21/10/2026');
    expect(primera).toContain('Binance USDT → ERC20 → Hapi · Tercer aporte');
    expect(primera).toContain('$200.00');
    expect(primera).toContain('$4.42 2.21%');
    expect(tarjetas[1].querySelector('.comentario')).toBeNull();
    expect(texto('.tarjeta-total > span')).toBe('Total');
    expect(texto('.tarjeta-total .total-principal')).toBe('$575.00');
    expect(texto('.tarjeta-total-detalle')).toBe(
      'Neto recibido $559.13 · Costo de fondeo $15.87 2.76%',
    );
  });

  it('muestra la acción Registrar aporte cuando hay aportes', () => {
    responder(LISTA_EXCEL);

    expect(texto('.encabezado button')).toBe('Registrar aporte');
  });

  it('abre el formulario desde el estado vacío', () => {
    responder(LISTA_VACIA);
    expect(elemento.querySelector('app-aporte-form')).toBeNull();

    elemento.querySelector<HTMLButtonElement>('.vacio button')!.click();
    fixture.detectChanges();

    expect(elemento.querySelector('app-aporte-form')).not.toBeNull();
  });

  it('al registrar confirma, cierra el formulario y recarga historial y totales', () => {
    responder(LISTA_VACIA);
    elemento.querySelector<HTMLButtonElement>('.vacio button')!.click();
    fixture.detectChanges();

    const escribir = (id: string, valor: string) => {
      const campo = elemento.querySelector<HTMLInputElement>(`#${id}`)!;
      campo.value = valor;
      campo.dispatchEvent(new Event('input'));
    };
    escribir('aporte-fecha', '2026-10-21');
    escribir('aporte-monto', '200.00');
    escribir('aporte-neto', '195.58');
    elemento.querySelector<HTMLFormElement>('app-aporte-form form')!.dispatchEvent(new Event('submit'));
    http.expectOne({ method: 'POST', url: '/api/v1/aportes' }).flush(LISTA_EXCEL.aportes[0], {
      status: 201,
      statusText: 'Created',
    });
    fixture.detectChanges();

    expect(texto('.aviso')).toBe('✓ Aporte registrado correctamente.');
    expect(elemento.querySelector('app-aporte-form')).toBeNull();
    // Recarga silenciosa: no vuelve al estado de carga.
    expect(elemento.querySelector('p[role="status"]')).toBeNull();

    http.expectOne({ method: 'GET', url: '/api/v1/aportes' }).flush({
      ...LISTA_EXCEL,
      aportes: [LISTA_EXCEL.aportes[0]],
      totalAportado: 200,
      totalNetoBroker: 195.58,
      totalCostoFondeo: 4.42,
      porcentajeCostoFondeo: 2.21,
      porcentajeNetoBroker: 97.79,
    });
    fixture.detectChanges();

    expect(elemento.querySelectorAll('tbody tr').length).toBe(1);
    expect(texto('.cifra-principal')).toBe('$200.00');
  });
});
