package com.personal.inversiones.asignaciones.controlador;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.inversiones.asignaciones.dto.AsignacionRespuesta;
import com.personal.inversiones.asignaciones.dto.GuardarAsignacionSolicitud;
import com.personal.inversiones.asignaciones.dto.ListaAsignacionesRespuesta;
import com.personal.inversiones.asignaciones.servicio.AsignacionObjetivoServicio;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/asignaciones")
public class AsignacionObjetivoControlador {

	private final AsignacionObjetivoServicio asignacionServicio;

	public AsignacionObjetivoControlador(AsignacionObjetivoServicio asignacionServicio) {
		this.asignacionServicio = asignacionServicio;
	}

	@PutMapping("/{activoId}")
	public AsignacionRespuesta guardar(@PathVariable Long activoId,
			@Valid @RequestBody GuardarAsignacionSolicitud solicitud) {
		return asignacionServicio.guardar(activoId, solicitud);
	}

	@GetMapping
	public ListaAsignacionesRespuesta listar() {
		return asignacionServicio.listar();
	}

}
