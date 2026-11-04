import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Activo, GuardarActivo } from './activo.model';

@Injectable({ providedIn: 'root' })
export class ActivoService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/activos';

  /** Ordenados por símbolo ascendente (lo garantiza el backend). */
  listar(): Observable<Activo[]> {
    return this.http.get<Activo[]>(this.url);
  }

  obtener(id: number): Observable<Activo> {
    return this.http.get<Activo>(`${this.url}/${id}`);
  }

  crear(activo: GuardarActivo): Observable<Activo> {
    return this.http.post<Activo>(this.url, activo);
  }

  actualizar(id: number, activo: GuardarActivo): Observable<Activo> {
    return this.http.put<Activo>(`${this.url}/${id}`, activo);
  }
}
