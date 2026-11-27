import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CrearReferido, ListaReferidos, Referido } from './referido.model';

/** En el MVP los referidos no se editan ni se eliminan: solo se registran y consultan. */
@Injectable({ providedIn: 'root' })
export class ReferidoService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/referidos';

  listar(): Observable<ListaReferidos> {
    return this.http.get<ListaReferidos>(this.url);
  }

  obtener(id: number): Observable<Referido> {
    return this.http.get<Referido>(`${this.url}/${id}`);
  }

  registrar(referido: CrearReferido): Observable<Referido> {
    return this.http.post<Referido>(this.url, referido);
  }
}
