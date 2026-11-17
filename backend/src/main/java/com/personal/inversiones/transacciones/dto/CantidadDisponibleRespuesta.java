package com.personal.inversiones.transacciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CantidadDisponibleRespuesta(
		Long activoId,
		LocalDate fecha,
		BigDecimal cantidadDisponible) {
}
