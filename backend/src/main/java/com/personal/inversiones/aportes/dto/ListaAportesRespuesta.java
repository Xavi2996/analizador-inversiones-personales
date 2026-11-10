package com.personal.inversiones.aportes.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Historial de aportes con los totales calculados por el backend (04-contratos-api §9.2).
 * Sin aportes, la lista está vacía y todos los totales y porcentajes valen 0.
 */
public record ListaAportesRespuesta(
		List<AporteRespuesta> aportes,
		BigDecimal totalAportado,
		BigDecimal totalNetoBroker,
		BigDecimal totalCostoFondeo,
		BigDecimal porcentajeCostoFondeo,
		BigDecimal porcentajeNetoBroker) {
}
