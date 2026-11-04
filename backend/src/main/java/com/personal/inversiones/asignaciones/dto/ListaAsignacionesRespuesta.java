package com.personal.inversiones.asignaciones.dto;

import java.math.BigDecimal;
import java.util.List;

public record ListaAsignacionesRespuesta(
		List<AsignacionRespuesta> asignaciones,
		BigDecimal sumaPorcentajes,
		boolean sumaCorrecta) {
}
