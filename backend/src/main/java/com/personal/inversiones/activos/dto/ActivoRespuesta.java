package com.personal.inversiones.activos.dto;

import com.personal.inversiones.activos.modelo.Activo;
import com.personal.inversiones.activos.modelo.Moneda;
import com.personal.inversiones.activos.modelo.TipoActivo;

public record ActivoRespuesta(
		Long id,
		String simbolo,
		String nombre,
		TipoActivo tipoActivo,
		Moneda moneda,
		boolean simboloEditable) {

	public static ActivoRespuesta desde(Activo activo, boolean simboloEditable) {
		return new ActivoRespuesta(activo.getId(), activo.getSimbolo(), activo.getNombre(),
				activo.getTipoActivo(), activo.getMoneda(), simboloEditable);
	}

}
