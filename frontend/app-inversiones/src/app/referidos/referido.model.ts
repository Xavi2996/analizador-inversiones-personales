/**
 * Contratos de /api/v1/referidos (documentacion/04-contratos-api.md §11).
 * El valor recibido, el total y el conteo los calcula el backend; el frontend solo los muestra.
 */

export interface Referido {
  id: number;
  activoId: number;
  simbolo: string;
  nombreActivo: string;
  /** Formato AAAA-MM-DD. */
  fechaReferido: string;
  origen: string;
  cantidad: number;
  precioAlRecibir: number;
  /** cantidad × precio al recibir, redondeado a 2 decimales. Histórico: no cambia con el precio. */
  valorRecibido: number;
  comentario: string | null;
}

/** Cuerpo de POST /referidos. No incluye el valor recibido: lo calcula el backend. */
export interface CrearReferido {
  activoId: number;
  /** Formato AAAA-MM-DD. */
  fechaReferido: string;
  origen: string;
  cantidad: number;
  precioAlRecibir: number;
  comentario?: string | null;
}

/** Sin referidos, la lista está vacía, el total vale 0 y el conteo 0. */
export interface ListaReferidos {
  /** Ordenados por fecha y, a igual fecha, por id, ambos descendentes (lo garantiza el backend). */
  referidos: Referido[];
  totalValorRecibido: number;
  conteo: number;
}
