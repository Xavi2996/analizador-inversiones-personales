package com.personal.inversiones.referidos.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Historial de referidos con el total calculado por el backend (04-contratos-api §11.2).
 * Sin referidos, la lista está vacía, el total vale 0.00 y el conteo 0.
 */
public record ListaReferidosRespuesta(
		List<ReferidoRespuesta> referidos,
		BigDecimal totalValorRecibido,
		long conteo) {
}
