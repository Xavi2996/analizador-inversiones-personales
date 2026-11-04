package com.personal.inversiones.activos.controlador;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.inversiones.activos.dto.ActivoRespuesta;
import com.personal.inversiones.activos.dto.ActualizarActivoSolicitud;
import com.personal.inversiones.activos.dto.CrearActivoSolicitud;
import com.personal.inversiones.activos.servicio.ActivoServicio;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/activos")
public class ActivoControlador {

	private final ActivoServicio activoServicio;

	public ActivoControlador(ActivoServicio activoServicio) {
		this.activoServicio = activoServicio;
	}

	@PostMapping
	public ResponseEntity<ActivoRespuesta> crear(@Valid @RequestBody CrearActivoSolicitud solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED).body(activoServicio.crear(solicitud));
	}

	@GetMapping
	public List<ActivoRespuesta> listar() {
		return activoServicio.listar();
	}

	@GetMapping("/{id}")
	public ActivoRespuesta obtener(@PathVariable Long id) {
		return activoServicio.obtener(id);
	}

	@PutMapping("/{id}")
	public ActivoRespuesta actualizar(@PathVariable Long id,
			@Valid @RequestBody ActualizarActivoSolicitud solicitud) {
		return activoServicio.actualizar(id, solicitud);
	}

}
