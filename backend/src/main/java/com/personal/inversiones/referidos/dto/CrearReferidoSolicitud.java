package com.personal.inversiones.referidos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * El valor recibido no forma parte de la solicitud: lo calcula el backend y, si llega, se ignora.
 * Los textos se normalizan al construir la solicitud: el origen se recorta (vacío sigue siendo inválido)
 * y el comentario se recorta (vacío → null). La fecha no futura la valida el servicio.
 */
public record CrearReferidoSolicitud(
		@NotNull(message = "Es obligatorio.")
		Long activoId,

		@NotNull(message = "Es obligatorio.")
		LocalDate fechaReferido,

		@NotBlank(message = "Es obligatorio.")
		@Size(max = 100, message = "Admite como máximo 100 caracteres.")
		String origen,

		@NotNull(message = "Es obligatorio.")
		@DecimalMin(value = "0", inclusive = false, message = "Debe ser mayor que cero.")
		@Digits(integer = 16, fraction = 8, message = "Admite como máximo 8 decimales y 16 dígitos enteros.")
		BigDecimal cantidad,

		@NotNull(message = "Es obligatorio.")
		@DecimalMin(value = "0", inclusive = false, message = "Debe ser mayor que cero.")
		@Digits(integer = 13, fraction = 6, message = "Admite como máximo 6 decimales y 13 dígitos enteros.")
		BigDecimal precioAlRecibir,

		@Size(max = 500, message = "Admite como máximo 500 caracteres.")
		String comentario) {

	public CrearReferidoSolicitud {
		origen = origen == null ? null : origen.strip();
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
