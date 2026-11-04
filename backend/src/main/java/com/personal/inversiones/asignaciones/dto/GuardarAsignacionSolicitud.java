package com.personal.inversiones.asignaciones.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * Los decimales se limitan a 4, igual que la columna {@code porcentaje_objetivo NUMERIC(8,4)}.
 */
public record GuardarAsignacionSolicitud(
		@NotNull(message = "Es obligatorio.")
		@DecimalMin(value = "0", message = "Debe estar entre 0 y 100.")
		@DecimalMax(value = "100", message = "Debe estar entre 0 y 100.")
		@Digits(integer = 4, fraction = 4, message = "Admite como máximo 4 decimales.")
		BigDecimal porcentajeObjetivo) {
}
