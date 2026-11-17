import { HttpErrorResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import {
  CantidadDisponible,
  CrearTransaccion,
  ListaTransacciones,
  Transaccion,
} from './transaccion.model';
import { TransaccionService } from './transaccion.service';

const TRANSACCION: Transaccion = {
  id: 5,
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
  comentario: 'Compra IVV',
};

const NUEVA_TRANSACCION: CrearTransaccion = {
  activoId: 1,
  tipoTransaccion: 'COMPRA',
  fechaTransaccion: '2026-10-22',
  cantidad: 0.14553,
  precioUnitario: 769.57,
  comision: 0.15,
  comentario: 'Compra IVV',
};

const LISTA: ListaTransacciones = {
  transacciones: [TRANSACCION],
  totalComprado: 112.0,
  totalVendido: 0,
  totalComisiones: 0.15,
  conteoCompras: 1,
  conteoVentas: 0,
};

const DISPONIBLE: CantidadDisponible = {
  activoId: 1,
  fecha: '2026-10-22',
  cantidadDisponible: 0.54308,
};

describe('TransaccionService', () => {
  let servicio: TransaccionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(TransaccionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista las transacciones sin filtros con GET /api/v1/transacciones', () => {
    let resultado: ListaTransacciones | undefined;
    servicio.listar().subscribe((lista) => (resultado = lista));

    const solicitud = http.expectOne('/api/v1/transacciones');
    expect(solicitud.request.method).toBe('GET');
    expect(solicitud.request.params.keys()).toEqual([]);
    solicitud.flush(LISTA);

    expect(resultado).toEqual(LISTA);
  });

  it('envía los filtros activoId y tipo como parámetros', () => {
    servicio.listar({ activoId: 1, tipo: 'VENTA' }).subscribe();

    const solicitud = http.expectOne((r) => r.url === '/api/v1/transacciones');
    expect(solicitud.request.params.get('activoId')).toBe('1');
    expect(solicitud.request.params.get('tipo')).toBe('VENTA');
    solicitud.flush(LISTA);
  });

  it('no envía los filtros nulos', () => {
    servicio.listar({ activoId: null, tipo: 'COMPRA' }).subscribe();

    const solicitud = http.expectOne((r) => r.url === '/api/v1/transacciones');
    expect(solicitud.request.params.has('activoId')).toBe(false);
    expect(solicitud.request.params.get('tipo')).toBe('COMPRA');
    solicitud.flush(LISTA);
  });

  it('obtiene una transacción con GET /api/v1/transacciones/{id}', () => {
    let resultado: Transaccion | undefined;
    servicio.obtener(5).subscribe((transaccion) => (resultado = transaccion));

    const solicitud = http.expectOne('/api/v1/transacciones/5');
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush(TRANSACCION);

    expect(resultado).toEqual(TRANSACCION);
  });

  it('registra una transacción con POST sin enviar monto bruto ni efectivo neto', () => {
    let resultado: Transaccion | undefined;
    servicio.registrar(NUEVA_TRANSACCION).subscribe((transaccion) => (resultado = transaccion));

    const solicitud = http.expectOne('/api/v1/transacciones');
    expect(solicitud.request.method).toBe('POST');
    expect(solicitud.request.body).toEqual(NUEVA_TRANSACCION);
    expect(solicitud.request.body).not.toHaveProperty('montoBruto');
    expect(solicitud.request.body).not.toHaveProperty('efectivoNeto');
    solicitud.flush(TRANSACCION, { status: 201, statusText: 'Created' });

    expect(resultado).toEqual(TRANSACCION);
  });

  it('consulta la cantidad disponible con activoId y fecha', () => {
    let resultado: CantidadDisponible | undefined;
    servicio.disponible(1, '2026-10-22').subscribe((disponible) => (resultado = disponible));

    const solicitud = http.expectOne((r) => r.url === '/api/v1/transacciones/disponible');
    expect(solicitud.request.method).toBe('GET');
    expect(solicitud.request.params.get('activoId')).toBe('1');
    expect(solicitud.request.params.get('fecha')).toBe('2026-10-22');
    solicitud.flush(DISPONIBLE);

    expect(resultado).toEqual(DISPONIBLE);
  });

  it('consulta la cantidad disponible sin fecha para que el backend use hoy', () => {
    servicio.disponible(1).subscribe();

    const solicitud = http.expectOne((r) => r.url === '/api/v1/transacciones/disponible');
    expect(solicitud.request.params.get('activoId')).toBe('1');
    expect(solicitud.request.params.has('fecha')).toBe(false);
    solicitud.flush(DISPONIBLE);
  });

  it('propaga el 409 CANTIDAD_INSUFICIENTE con su mensaje y campo', () => {
    let error: HttpErrorResponse | undefined;
    servicio
      .registrar({ ...NUEVA_TRANSACCION, tipoTransaccion: 'VENTA', cantidad: 1 })
      .subscribe({ error: (e: HttpErrorResponse) => (error = e) });

    const cuerpoError = {
      estadoHttp: 409,
      codigo: 'CANTIDAD_INSUFICIENTE',
      mensaje: 'La cantidad supera la disponible para vender en esa fecha (0.54308000 IVV).',
      erroresCampos: [{ campo: 'cantidad', mensaje: 'La cantidad supera la disponible para vender en esa fecha (0.54308000 IVV).' }],
    };
    http
      .expectOne('/api/v1/transacciones')
      .flush(cuerpoError, { status: 409, statusText: 'Conflict' });

    expect(error?.status).toBe(409);
    expect(error?.error).toEqual(cuerpoError);
  });

  it('propaga el 404 TRANSACCION_NO_ENCONTRADA', () => {
    let error: HttpErrorResponse | undefined;
    servicio.obtener(999).subscribe({ error: (e: HttpErrorResponse) => (error = e) });

    http
      .expectOne('/api/v1/transacciones/999')
      .flush({ codigo: 'TRANSACCION_NO_ENCONTRADA' }, { status: 404, statusText: 'Not Found' });

    expect(error?.status).toBe(404);
    expect(error?.error.codigo).toBe('TRANSACCION_NO_ENCONTRADA');
  });
});
