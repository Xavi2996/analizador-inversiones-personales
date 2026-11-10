import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Aporte, CrearAporte, ListaAportes } from './aporte.model';

/** En el MVP los aportes no se editan ni se eliminan: solo se registran y consultan. */
@Injectable({ providedIn: 'root' })
export class AporteService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/aportes';

  listar(): Observable<ListaAportes> {
    return this.http.get<ListaAportes>(this.url);
  }

  obtener(id: number): Observable<Aporte> {
    return this.http.get<Aporte>(`${this.url}/${id}`);
  }

  registrar(aporte: CrearAporte): Observable<Aporte> {
    return this.http.post<Aporte>(this.url, aporte);
  }
}
