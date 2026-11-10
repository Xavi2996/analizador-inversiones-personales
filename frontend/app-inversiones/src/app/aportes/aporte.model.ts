/**
 * Contratos de /api/v1/aportes (documentacion/04-contratos-api.md §9).
 * Los montos, el costo de fondeo, los totales y los porcentajes los calcula el backend;
 * el frontend solo los muestra.
 */

export interface Aporte {
  id: number;
  /** Formato AAAA-MM-DD. */
  fechaAporte: string;
  montoAportado: number;
  montoNetoBroker: number;
  costoFondeo: number;
  porcentajeCostoFondeo: number;
  metodoFondeo: string | null;
  comentario: string | null;
}

/** Cuerpo de POST /aportes. No incluye el costo de fondeo: lo calcula el backend. */
export interface CrearAporte {
  /** Formato AAAA-MM-DD. */
  fechaAporte: string;
  montoAportado: number;
  montoNetoBroker: number;
  metodoFondeo?: string | null;
  comentario?: string | null;
}

/** Sin aportes, la lista está vacía y todos los totales y porcentajes valen 0. */
export interface ListaAportes {
  /** Ordenados por fecha y, a igual fecha, por id, ambos descendentes (lo garantiza el backend). */
  aportes: Aporte[];
  totalAportado: number;
  totalNetoBroker: number;
  totalCostoFondeo: number;
  porcentajeCostoFondeo: number;
  porcentajeNetoBroker: number;
}
