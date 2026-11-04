import { HttpErrorResponse } from '@angular/common/http';

/** Estructura común de error de la API (documentacion/04-contratos-api.md §7). */
export interface ErrorApi {
  fechaHora: string;
  estadoHttp: number;
  codigo: string;
  mensaje: string;
  ruta: string;
  erroresCampos?: ErrorCampoApi[];
}

export interface ErrorCampoApi {
  campo: string;
  mensaje: string;
}

/** Devuelve el cuerpo de error de la API, o null si la respuesta no tiene esa estructura (red caída, proxy, etc.). */
export function leerErrorApi(error: unknown): ErrorApi | null {
  if (error instanceof HttpErrorResponse && error.error && typeof error.error.codigo === 'string') {
    return error.error as ErrorApi;
  }
  return null;
}
