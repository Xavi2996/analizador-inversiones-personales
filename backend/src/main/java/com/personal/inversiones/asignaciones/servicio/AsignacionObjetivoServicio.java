package com.personal.inversiones.asignaciones.servicio;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.inversiones.activos.modelo.Activo;
import com.personal.inversiones.activos.servicio.ActivoServicio;
import com.personal.inversiones.asignaciones.dto.AsignacionRespuesta;
import com.personal.inversiones.asignaciones.dto.GuardarAsignacionSolicitud;
import com.personal.inversiones.asignaciones.dto.ListaAsignacionesRespuesta;
import com.personal.inversiones.asignaciones.modelo.AsignacionObjetivo;
import com.personal.inversiones.asignaciones.repositorio.AsignacionObjetivoRepositorio;

@Service
public class AsignacionObjetivoServicio {

	/** Escala de la columna {@code porcentaje_objetivo}; mantiene igual la respuesta del PUT y la del GET. */
	private static final int ESCALA = 4;
	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private final AsignacionObjetivoRepositorio asignacionRepositorio;
	private final ActivoServicio activoServicio;

	public AsignacionObjetivoServicio(AsignacionObjetivoRepositorio asignacionRepositorio,
			ActivoServicio activoServicio) {
		this.asignacionRepositorio = asignacionRepositorio;
		this.activoServicio = activoServicio;
	}

	/**
	 * Crea la asignación del activo o actualiza la existente; nunca hay más de una fila por activo.
	 */
	@Transactional
	public AsignacionRespuesta guardar(Long activoId, GuardarAsignacionSolicitud solicitud) {
		Activo activo = activoServicio.buscar(activoId);
		// La validación ya garantiza como máximo 4 decimales, así que la escala no redondea.
		BigDecimal porcentaje = solicitud.porcentajeObjetivo().setScale(ESCALA);
		AsignacionObjetivo asignacion = asignacionRepositorio.findByActivoId(activoId)
				.map(existente -> {
					existente.actualizarPorcentaje(porcentaje);
					return existente;
				})
				.orElseGet(() -> new AsignacionObjetivo(activo, porcentaje));
		return AsignacionRespuesta.desde(asignacionRepositorio.save(asignacion));
	}

	/**
	 * Un activo sin asignación no aparece en la lista y cuenta como 0 % en la suma.
	 */
	@Transactional(readOnly = true)
	public ListaAsignacionesRespuesta listar() {
		List<AsignacionRespuesta> asignaciones = asignacionRepositorio.findAllConActivoOrdenadasPorSimbolo().stream()
				.map(AsignacionRespuesta::desde)
				.toList();
		BigDecimal suma = asignaciones.stream()
				.map(AsignacionRespuesta::porcentajeObjetivo)
				.reduce(BigDecimal.ZERO.setScale(ESCALA), BigDecimal::add);
		return new ListaAsignacionesRespuesta(asignaciones, suma, suma.compareTo(CIEN) == 0);
	}

}
