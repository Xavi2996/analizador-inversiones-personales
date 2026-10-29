import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Activo, GuardarActivo } from './activo.model';
import { ActivoService } from './activo.service';

const IVV: Activo = {
  id: 1,
  simbolo: 'IVV',
  nombre: 'iShares Core S&P 500 ETF',
  tipoActivo: 'ETF',
  moneda: 'USD',
  simboloEditable: true,
};

const DATOS_IVV: GuardarActivo = {
  simbolo: 'IVV',
  nombre: 'iShares Core S&P 500 ETF',
  tipoActivo: 'ETF',
  moneda: 'USD',
};

describe('ActivoService', () => {
  let servicio: ActivoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(ActivoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista los activos con GET /api/v1/activos', () => {
    let resultado: Activo[] | undefined;
    servicio.listar().subscribe((activos) => (resultado = activos));

    const solicitud = http.expectOne('/api/v1/activos');
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush([IVV]);

    expect(resultado).toEqual([IVV]);
  });

  it('obtiene un activo con GET /api/v1/activos/{id}', () => {
    let resultado: Activo | undefined;
    servicio.obtener(1).subscribe((activo) => (resultado = activo));

    const solicitud = http.expectOne('/api/v1/activos/1');
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush(IVV);

    expect(resultado).toEqual(IVV);
  });

  it('crea un activo con POST /api/v1/activos', () => {
    let resultado: Activo | undefined;
    servicio.crear(DATOS_IVV).subscribe((activo) => (resultado = activo));

    const solicitud = http.expectOne('/api/v1/activos');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual(DATOS_IVV);
    solicitud.flush(IVV, { status: 201, statusText: 'Created' });

    expect(resultado).toEqual(IVV);
  });

  it('actualiza un activo con PUT /api/v1/activos/{id}', () => {
    const cambios: GuardarActivo = { ...DATOS_IVV, nombre: 'Nombre corregido' };
    servicio.actualizar(1, cambios).subscribe();

    const solicitud = http.expectOne('/api/v1/activos/1');
    expect(solicitud.request.method).toBe('PUT');
    expect(solicitud.request.body).toEqual(cambios);
    solicitud.flush({ ...IVV, nombre: 'Nombre corregido' });
  });

  it('propaga el error de la API', () => {
    let estado: number | undefined;
    servicio.crear(DATOS_IVV).subscribe({ error: (error) => (estado = error.status) });

    http
      .expectOne('/api/v1/activos')
      .flush({ codigo: 'SIMBOLO_DUPLICADO' }, { status: 409, statusText: 'Conflict' });

    expect(estado).toBe(409);
  });
});
