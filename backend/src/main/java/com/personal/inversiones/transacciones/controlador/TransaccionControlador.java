package com.personal.inversiones.transacciones.controlador;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.personal.inversiones.transacciones.dto.CantidadDisponibleRespuesta;
import com.personal.inversiones.transacciones.dto.CrearTransaccionSolicitud;
import com.personal.inversiones.transacciones.dto.TransaccionRespuesta;
import com.personal.inversiones.transacciones.servicio.TransaccionServicio;

import jakarta.validation.Valid;

/**
 * En el MVP las transacciones no se editan ni se eliminan: no existen PUT ni DELETE.
 */
@RestController
@RequestMapping("/api/v1/transacciones")
public class TransaccionControlador {

	private final TransaccionServicio transaccionServicio;

	public TransaccionControlador(TransaccionServicio transaccionServicio) {
		this.transaccionServicio = transaccionServicio;
	}

	@PostMapping
	public ResponseEntity<TransaccionRespuesta> registrar(@Valid @RequestBody CrearTransaccionSolicitud solicitud) {
		return ResponseEntity.status(HttpStatus.CREATED).body(transaccionServicio.registrar(solicitud));
	}

	/**
	 * {@code activoId} se declara opcional para que su ausencia responda 400 {@code DATOS_INVALIDOS}
	 * con el campo informado (04-contratos-api §10.5), igual que el resto de validaciones.
	 */
	@GetMapping("/disponible")
	public CantidadDisponibleRespuesta disponible(@RequestParam(required = false) Long activoId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		return transaccionServicio.disponible(activoId, fecha);
	}

}
