import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Aporte, CrearAporte, ListaAportes } from './aporte.model';
import { AporteService } from './aporte.service';

const APORTE: Aporte = {
  id: 3,
  fechaAporte: '2026-10-21',
  montoAportado: 200.0,
  montoNetoBroker: 195.58,
  costoFondeo: 4.42,
  porcentajeCostoFondeo: 2.21,
  metodoFondeo: 'Binance USDT → ERC20 → Hapi',
  comentario: 'Tercer aporte',
};

const NUEVO_APORTE: CrearAporte = {
  fechaAporte: '2026-10-21',
  montoAportado: 200.0,
  montoNetoBroker: 195.58,
  metodoFondeo: 'Binance USDT → ERC20 → Hapi',
  comentario: 'Tercer aporte',
};

const LISTA: ListaAportes = {
  aportes: [APORTE],
  totalAportado: 200.0,
  totalNetoBroker: 195.58,
  totalCostoFondeo: 4.42,
  porcentajeCostoFondeo: 2.21,
  porcentajeNetoBroker: 97.79,
};

describe('AporteService', () => {
  let servicio: AporteService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(AporteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista los aportes con sus totales con GET /api/v1/aportes', () => {
    let resultado: ListaAportes | undefined;
    servicio.listar().subscribe((lista) => (resultado = lista));

    const solicitud = http.expectOne('/api/v1/aportes');
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush(LISTA);

    expect(resultado).toEqual(LISTA);
  });

  it('obtiene un aporte con GET /api/v1/aportes/{id}', () => {
    let resultado: Aporte | undefined;
    servicio.obtener(3).subscribe((aporte) => (resultado = aporte));

    const solicitud = http.expectOne('/api/v1/aportes/3');
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush(APORTE);

    expect(resultado).toEqual(APORTE);
  });

  it('registra un aporte con POST /api/v1/aportes sin enviar el costo de fondeo', () => {
    let resultado: Aporte | undefined;
    servicio.registrar(NUEVO_APORTE).subscribe((aporte) => (resultado = aporte));

    const solicitud = http.expectOne('/api/v1/aportes');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual(NUEVO_APORTE);
    expect(solicitud.request.body).not.toHaveProperty('costoFondeo');
    solicitud.flush(APORTE, { status: 201, statusText: 'Created' });

    expect(resultado).toEqual(APORTE);
  });

  it('propaga el error 400 con los errores de campo del backend', () => {
    let error: HttpErrorResponse | undefined;
    servicio.registrar({ ...NUEVO_APORTE, montoNetoBroker: 250 }).subscribe({
      error: (e: HttpErrorResponse) => (error = e),
    });

    const cuerpoError = {
      estadoHttp: 400,
      codigo: 'DATOS_INVALIDOS',
      mensaje: 'La solicitud contiene datos inválidos.',
      erroresCampos: [{ campo: 'montoNetoBroker', mensaje: 'No puede ser mayor al monto aportado.' }],
    };
    http
      .expectOne('/api/v1/aportes')
      .flush(cuerpoError, { status: 400, statusText: 'Bad Request' });

    expect(error?.status).toBe(400);
    expect(error?.error).toEqual(cuerpoError);
  });

  it('propaga el 404 APORTE_NO_ENCONTRADO', () => {
    let error: HttpErrorResponse | undefined;
    servicio.obtener(999).subscribe({ error: (e: HttpErrorResponse) => (error = e) });

    http
      .expectOne('/api/v1/aportes/999')
      .flush({ codigo: 'APORTE_NO_ENCONTRADO' }, { status: 404, statusText: 'Not Found' });

    expect(error?.status).toBe(404);
    expect(error?.error.codigo).toBe('APORTE_NO_ENCONTRADO');
  });
});
