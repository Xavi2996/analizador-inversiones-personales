package com.personal.inversiones.referidos.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.inversiones.referidos.dto.CrearReferidoSolicitud;
import com.personal.inversiones.referidos.dto.ReferidoRespuesta;
import com.personal.inversiones.referidos.servicio.ReferidoServicio;

import jakarta.validation.Valid;

/**
 * En el MVP los referidos no se editan ni se eliminan: no existen PUT ni DELETE.
 */
@RestController
@RequestMapping("/api/v1/referidos")
public class ReferidoControlador {

	private final ReferidoServicio referidoServicio;

	public ReferidoControlador(ReferidoServicio referidoServicio) {
		this.referidoServicio = referidoServicio;
	}

	@PostMapping
	public ResponseEntity<ReferidoRespuesta> registrar(@Valid @RequestBody CrearReferidoSolicitud solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED).body(referidoServicio.registrar(solicitud));
	}

}
