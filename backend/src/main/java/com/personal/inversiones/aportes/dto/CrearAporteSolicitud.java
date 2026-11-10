package com.personal.inversiones.aportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * El costo de fondeo no forma parte de la solicitud: lo calcula el backend y, si llega, se ignora.
 * Los textos opcionales se normalizan al construir la solicitud (sin espacios en los extremos; vacío → null),
 * así las validaciones se aplican sobre el valor que se guardará.
 * La fecha no futura y "neto ≤ aportado" las valida el servicio.
 */
public record CrearAporteSolicitud(
		@NotNull(message = "Es obligatorio.")
		LocalDate fechaAporte,

		@NotNull(message = "Es obligatorio.")
		@DecimalMin(value = "0", inclusive = false, message = "Debe ser mayor que cero.")
		@Digits(integer = 13, fraction = 2, message = "Admite como máximo 2 decimales y 13 dígitos enteros.")
		BigDecimal montoAportado,

		@NotNull(message = "Es obligatorio.")
		@DecimalMin(value = "0", message = "No puede ser negativo.")
		@Digits(integer = 13, fraction = 2, message = "Admite como máximo 2 decimales y 13 dígitos enteros.")
		BigDecimal montoNetoBroker,

		@Size(max = 50, message = "Admite como máximo 50 caracteres.")
		String metodoFondeo,

		@Size(max = 500, message = "Admite como máximo 500 caracteres.")
		String comentario) {

	public CrearAporteSolicitud {
		metodoFondeo = normalizarTextoOpcional(metodoFondeo);
		comentario = normalizarTextoOpcional(comentario);
	}

	private static String normalizarTextoOpcional(String texto) {
		if (texto == null) {
			return null;
		}
		String limpio = texto.strip();
		return limpio.isEmpty() ? null : limpio;
	}

}
