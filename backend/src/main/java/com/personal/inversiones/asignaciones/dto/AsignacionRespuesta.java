package com.personal.inversiones.asignaciones.dto;

import java.math.BigDecimal;

import com.personal.inversiones.asignaciones.modelo.AsignacionObjetivo;

public record AsignacionRespuesta(
		Long activoId,
		String simbolo,
		BigDecimal porcentajeObjetivo) {

	public static AsignacionRespuesta desde(AsignacionObjetivo asignacion) {
		return new AsignacionRespuesta(asignacion.getActivo().getId(), asignacion.getActivo().getSimbolo(),
				asignacion.getPorcentajeObjetivo());
	}

}
