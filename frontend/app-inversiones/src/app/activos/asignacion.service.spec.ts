import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Asignacion, ListaAsignaciones } from './activo.model';
import { AsignacionService } from './asignacion.service';

describe('AsignacionService', () => {
  let servicio: AsignacionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(AsignacionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista las asignaciones con su suma con GET /api/v1/asignaciones', () => {
    const respuesta: ListaAsignaciones = {
      asignaciones: [
        { activoId: 3, simbolo: 'BTC', porcentajeObjetivo: 5 },
        { activoId: 1, simbolo: 'IVV', porcentajeObjetivo: 85 },
        { activoId: 2, simbolo: 'VXUS', porcentajeObjetivo: 10 },
      ],
      sumaPorcentajes: 100,
      sumaCorrecta: true,
    };
    let resultado: ListaAsignaciones | undefined;
    servicio.listar().subscribe((lista) => (resultado = lista));

    const solicitud = http.expectOne('/api/v1/asignaciones');
    expect(solicitud.request.method).toBe('GET');
    solicitud.flush(respuesta);

    expect(resultado).toEqual(respuesta);
  });

  it('guarda la asignación con PUT /api/v1/asignaciones/{activoId}', () => {
    let resultado: Asignacion | undefined;
    servicio.guardar(1, 85).subscribe((asignacion) => (resultado = asignacion));

    const solicitud = http.expectOne('/api/v1/asignaciones/1');
    expect(solicitud.request.method).toBe('PUT');
    expect(solicitud.request.body).toEqual({ porcentajeObjetivo: 85 });
    solicitud.flush({ activoId: 1, simbolo: 'IVV', porcentajeObjetivo: 85 });

    expect(resultado).toEqual({ activoId: 1, simbolo: 'IVV', porcentajeObjetivo: 85 });
  });
});
