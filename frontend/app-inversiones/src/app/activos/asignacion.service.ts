import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Asignacion, ListaAsignaciones } from './activo.model';

@Injectable({ providedIn: 'root' })
export class AsignacionService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/asignaciones';

  listar(): Observable<ListaAsignaciones> {
    return this.http.get<ListaAsignaciones>(this.url);
  }

  /** Crea la asignación del activo o actualiza la existente. */
  guardar(activoId: number, porcentajeObjetivo: number): Observable<Asignacion> {
    return this.http.put<Asignacion>(`${this.url}/${activoId}`, { porcentajeObjetivo });
  }
}
