package com.personal.inversiones.aportes.controlador;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.personal.inversiones.aportes.dto.AporteRespuesta;
import com.personal.inversiones.aportes.dto.CrearAporteSolicitud;
import com.personal.inversiones.aportes.servicio.AporteServicio;

import jakarta.validation.Valid;

/**
 * En el MVP los aportes no se editan ni se eliminan: no existen PUT ni DELETE.
 */
@RestController
@RequestMapping("/api/v1/aportes")
public class AporteControlador {

	private final AporteServicio aporteServicio;

	public AporteControlador(AporteServicio aporteServicio) {
		this.aporteServicio = aporteServicio;
	}

	@PostMapping
	public ResponseEntity<AporteRespuesta> registrar(@Valid @RequestBody CrearAporteSolicitud solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED).body(aporteServicio.registrar(solicitud));
	}

}
