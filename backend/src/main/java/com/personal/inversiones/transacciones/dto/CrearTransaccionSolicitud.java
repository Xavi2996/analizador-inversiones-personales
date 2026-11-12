package com.personal.inversiones.transacciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.personal.inversiones.transacciones.modelo.TipoTransaccion;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * El monto bruto y el efectivo neto no forman parte de la solicitud: los calcula el backend y, si llegan,
 * se ignoran. El comentario se normaliza al construir la solicitud (sin espacios en los extremos; vacío → null).
 * La fecha no futura y las reglas de venta las valida el servicio.
 */
public record CrearTransaccionSolicitud(
		@NotNull(message = "Es obligatorio.")
		Long activoId,

		@NotNull(message = "Es obligatorio.")
		TipoTransaccion tipoTransaccion,

		@NotNull(message = "Es obligatorio.")
		LocalDate fechaTransaccion,

		@NotNull(message = "Es obligatorio.")
		@DecimalMin(value = "0", inclusive = false, message = "Debe ser mayor que cero.")
		@Digits(integer = 16, fraction = 8, message = "Admite como máximo 8 decimales y 16 dígitos enteros.")
		BigDecimal cantidad,

		@NotNull(message = "Es obligatorio.")
		@DecimalMin(value = "0", inclusive = false, message = "Debe ser mayor que cero.")
		@Digits(integer = 13, fraction = 6, message = "Admite como máximo 6 decimales y 13 dígitos enteros.")
		BigDecimal precioUnitario,

		@NotNull(message = "Es obligatorio.")
		@DecimalMin(value = "0", message = "No puede ser negativo.")
		@Digits(integer = 13, fraction = 2, message = "Admite como máximo 2 decimales y 13 dígitos enteros.")
		BigDecimal comision,

		@Size(max = 500, message = "Admite como máximo 500 caracteres.")
		String comentario) {

	public CrearTransaccionSolicitud {
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
