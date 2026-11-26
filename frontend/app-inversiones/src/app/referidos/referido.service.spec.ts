import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CrearReferido, ListaReferidos, Referido } from './referido.model';
import { ReferidoService } from './referido.service';

const REFERIDO: Referido = {
  id: 3,
  activoId: 1,
  simbolo: 'BTC',
  nombreActivo: 'Bitcoin',
  fechaReferido: '2026-10-13',
  origen: 'Programa de referidos Hapi',
  cantidad: 0.00001196,
  precioAlRecibir: 83580.27,
  valorRecibido: 1.0,
  comentario: null,
};

const NUEVO_REFERIDO: CrearReferido = {
  activoId: 1,
  fechaReferido: '2026-10-13',
  origen: 'Programa de referidos Hapi',
  cantidad: 0.00001196,
  precioAlRecibir: 83580.27,
  comentario: null,
};

const LISTA: ListaReferidos = {
  referidos: [REFERIDO],
  totalValorRecibido: 1.0,
  conteo: 1,
};

describe('ReferidoService', () => {
  let servicio: ReferidoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(ReferidoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista los referidos con GET /api/v1/referidos sin parámetros', () => {
    let resultado: ListaReferidos | undefined;
    servicio.listar().subscribe((lista) => (resultado = lista));

    const solicitud = http.expectOne('/api/v1/referidos');
    expect(solicitud.request.method).toBe('GET');
    expect(solicitud.request.params.keys()).toEqual([]);
    solicitud.flush(LISTA);

    expect(resultado).toEqual(LISTA);
  });

  it('obtiene un referido con GET /api/v1/referidos/{id}', () => {
    let resultado: Referido | undefined;
    servicio.obtener(3).subscribe((referido) => (resultado = referido));

    const solicitud = http.expectOne('/api/v1/referidos/3');
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush(REFERIDO);

    expect(resultado).toEqual(REFERIDO);
  });

  it('registra un referido con POST sin enviar el valor recibido', () => {
    let resultado: Referido | undefined;
    servicio.registrar(NUEVO_REFERIDO).subscribe((referido) => (resultado = referido));

    const solicitud = http.expectOne('/api/v1/referidos');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual(NUEVO_REFERIDO);
    expect(solicitud.request.body).not.toHaveProperty('valorRecibido');
    solicitud.flush(REFERIDO, { status: 201, statusText: 'Created' });

    expect(resultado).toEqual(REFERIDO);
  });

  it('propaga el 400 DATOS_INVALIDOS con sus errores de campo', () => {
    let error: HttpErrorResponse | undefined;
    servicio
      .registrar({ ...NUEVO_REFERIDO, origen: '' })
      .subscribe({ error: (e: HttpErrorResponse) => (error = e) });

    const cuerpoError = {
      estadoHttp: 400,
      codigo: 'DATOS_INVALIDOS',
      mensaje: 'La solicitud contiene datos inválidos.',
      erroresCampos: [{ campo: 'origen', mensaje: 'Es obligatorio.' }],
    };
    http.expectOne('/api/v1/referidos').flush(cuerpoError, { status: 400, statusText: 'Bad Request' });

    expect(error?.status).toBe(400);
    expect(error?.error).toEqual(cuerpoError);
  });

  it('propaga el 404 REFERIDO_NO_ENCONTRADO', () => {
    let error: HttpErrorResponse | undefined;
    servicio.obtener(999).subscribe({ error: (e: HttpErrorResponse) => (error = e) });

    http
      .expectOne('/api/v1/referidos/999')
      .flush({ codigo: 'REFERIDO_NO_ENCONTRADO' }, { status: 404, statusText: 'Not Found' });

    expect(error?.status).toBe(404);
    expect(error?.error.codigo).toBe('REFERIDO_NO_ENCONTRADO');
  });
});
