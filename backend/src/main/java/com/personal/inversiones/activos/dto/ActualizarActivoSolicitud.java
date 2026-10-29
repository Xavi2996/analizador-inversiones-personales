package com.personal.inversiones.activos.dto;

import java.util.Locale;

import com.personal.inversiones.activos.modelo.Moneda;
import com.personal.inversiones.activos.modelo.TipoActivo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Mismas reglas que {@link CrearActivoSolicitud}: todos los campos son obligatorios
 * y el símbolo y el nombre se normalizan antes de validar.
 */
public record ActualizarActivoSolicitud(
		@NotBlank(message = "Es obligatorio.")
		// {0,20}: el vacío ya lo informa @NotBlank y así no se duplica el error.
		@Pattern(regexp = "[A-Z0-9.-]{0,20}",
				message = "Solo se permiten letras, números, punto y guion (máximo 20).")
		String simbolo,

		@NotBlank(message = "Es obligatorio.")
		@Size(max = 150, message = "Admite como máximo 150 caracteres.")
		String nombre,

		@NotNull(message = "Es obligatorio.")
		TipoActivo tipoActivo,

		@NotNull(message = "Es obligatorio.")
		Moneda moneda) {

	public ActualizarActivoSolicitud {
		simbolo = simbolo == null ? null : simbolo.strip().toUpperCase(Locale.ROOT);
		nombre = nombre == null ? null : nombre.strip();
	}

}
