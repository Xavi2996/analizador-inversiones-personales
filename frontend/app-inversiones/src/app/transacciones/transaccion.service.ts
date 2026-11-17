import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CantidadDisponible,
  CrearTransaccion,
  FiltroTransacciones,
  ListaTransacciones,
  Transaccion,
} from './transaccion.model';

/** En el MVP las transacciones no se editan ni se eliminan: solo se registran y consultan. */
@Injectable({ providedIn: 'root' })
export class TransaccionService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/transacciones';

  /** Los filtros ausentes no se envían. */
  listar(filtro: FiltroTransacciones = {}): Observable<ListaTransacciones> {
    let params = new HttpParams();
    if (filtro.activoId != null) {
      params = params.set('activoId', filtro.activoId);
    }
    if (filtro.tipo != null) {
      params = params.set('tipo', filtro.tipo);
    }
    return this.http.get<ListaTransacciones>(this.url, { params });
  }

  obtener(id: number): Observable<Transaccion> {
    return this.http.get<Transaccion>(`${this.url}/${id}`);
  }

  registrar(transaccion: CrearTransaccion): Observable<Transaccion> {
    return this.http.post<Transaccion>(this.url, transaccion);
  }

  /** Sin fecha, el backend usa la fecha actual de negocio. */
  disponible(activoId: number, fecha?: string | null): Observable<CantidadDisponible> {
    let params = new HttpParams().set('activoId', activoId);
    if (fecha) {
      params = params.set('fecha', fecha);
    }
    return this.http.get<CantidadDisponible>(`${this.url}/disponible`, { params });
  }
}
