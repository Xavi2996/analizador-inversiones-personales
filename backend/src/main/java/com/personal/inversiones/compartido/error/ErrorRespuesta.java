package com.personal.inversiones.compartido.error;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Estructura común de error de la API (04-contratos-api §7).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(
		OffsetDateTime fechaHora,
		int estadoHttp,
		String codigo,
		String mensaje,
		String ruta,
		List<ErrorCampo> erroresCampos) {
}
