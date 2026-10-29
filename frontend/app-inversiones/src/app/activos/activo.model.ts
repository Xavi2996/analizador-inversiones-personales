/**
 * Contratos de /api/v1/activos y /api/v1/asignaciones (documentacion/04-contratos-api.md §8 y §14).
 */

export type TipoActivo = 'ACCION' | 'ETF' | 'CRIPTOMONEDA';

export type Moneda = 'USD';

export interface Activo {
  id: number;
  simbolo: string;
  nombre: string;
  tipoActivo: TipoActivo;
  moneda: Moneda;
  /** Lo calcula el backend: false cuando el activo tiene registros históricos. */
  simboloEditable: boolean;
}

/** Cuerpo de POST /activos y de PUT /activos/{id}; en ambos todos los campos son obligatorios. */
export interface GuardarActivo {
  simbolo: string;
  nombre: string;
  tipoActivo: TipoActivo;
  moneda: Moneda;
}

export interface Asignacion {
  activoId: number;
  simbolo: string;
  porcentajeObjetivo: number;
}

/** La suma y su validez las calcula el backend; el frontend solo las muestra. */
export interface ListaAsignaciones {
  asignaciones: Asignacion[];
  sumaPorcentajes: number;
  sumaCorrecta: boolean;
}
