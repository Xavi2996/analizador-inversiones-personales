/**
 * Contratos de /api/v1/transacciones (documentacion/04-contratos-api.md §10).
 * El monto bruto, el efectivo neto, la cantidad disponible, los totales y los conteos los calcula
 * el backend; el frontend solo los muestra.
 */

export type TipoTransaccion = 'COMPRA' | 'VENTA';

export interface Transaccion {
  id: number;
  activoId: number;
  simbolo: string;
  nombreActivo: string;
  tipoTransaccion: TipoTransaccion;
  /** Formato AAAA-MM-DD. */
  fechaTransaccion: string;
  cantidad: number;
  precioUnitario: number;
  montoBruto: number;
  comision: number;
  /** Con signo: negativo en compras (−(monto + comisión)), positivo en ventas (monto − comisión). */
  efectivoNeto: number;
  comentario: string | null;
}

/** Cuerpo de POST /transacciones. No incluye monto bruto ni efectivo neto: los calcula el backend. */
export interface CrearTransaccion {
  activoId: number;
  tipoTransaccion: TipoTransaccion;
  /** Formato AAAA-MM-DD. */
  fechaTransaccion: string;
  cantidad: number;
  precioUnitario: number;
  comision: number;
  comentario?: string | null;
}

/** Filtros opcionales de GET /transacciones. */
export interface FiltroTransacciones {
  activoId?: number | null;
  tipo?: TipoTransaccion | null;
}

/**
 * Los totales se calculan sobre la lista filtrada por activo y tipo.
 * Los conteos solo aplican el filtro de activo (pestañas Todas / Compras / Ventas).
 * Sin resultados, la lista está vacía y totales y conteos valen 0.
 */
export interface ListaTransacciones {
  /** Ordenadas por fecha y, a igual fecha, por id, ambos descendentes (lo garantiza el backend). */
  transacciones: Transaccion[];
  totalComprado: number;
  totalVendido: number;
  totalComisiones: number;
  conteoCompras: number;
  conteoVentas: number;
}

/** Respuesta de GET /transacciones/disponible. */
export interface CantidadDisponible {
  activoId: number;
  /** Formato AAAA-MM-DD. */
  fecha: string;
  cantidadDisponible: number;
}
