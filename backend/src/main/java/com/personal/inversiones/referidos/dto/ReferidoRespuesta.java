package com.personal.inversiones.referidos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Solo datos históricos del referido. Precio actual, valor actual y variación llegan con Precios (SPEC 004 §2).
 */
public record ReferidoRespuesta(
		Long id,
		Long activoId,
		String simbolo,
		String nombreActivo,
		LocalDate fechaReferido,
		String origen,
		BigDecimal cantidad,
		BigDecimal precioAlRecibir,
		BigDecimal valorRecibido,
		String comentario) {
}
