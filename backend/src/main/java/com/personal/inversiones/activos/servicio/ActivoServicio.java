package com.personal.inversiones.activos.servicio;

import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.inversiones.activos.dto.ActivoRespuesta;
import com.personal.inversiones.activos.dto.CrearActivoSolicitud;
import com.personal.inversiones.activos.modelo.Activo;
import com.personal.inversiones.activos.repositorio.ActivoRepositorio;
import com.personal.inversiones.compartido.error.ConflictoExcepcion;
import com.personal.inversiones.compartido.error.RecursoNoEncontradoExcepcion;

@Service
public class ActivoServicio {

	public static final String ACTIVO_NO_ENCONTRADO = "ACTIVO_NO_ENCONTRADO";
	public static final String SIMBOLO_DUPLICADO = "SIMBOLO_DUPLICADO";

	private static final String RESTRICCION_SIMBOLO_UNICO = "uk_activo_simbolo";

	private final ActivoRepositorio activoRepositorio;

	public ActivoServicio(ActivoRepositorio activoRepositorio) {
		this.activoRepositorio = activoRepositorio;
	}

	@Transactional
	public ActivoRespuesta crear(CrearActivoSolicitud solicitud) {
		if (activoRepositorio.existsBySimbolo(solicitud.simbolo())) {
			throw simboloDuplicado(solicitud.simbolo());
		}
		Activo activo = new Activo(solicitud.simbolo(), solicitud.nombre(), solicitud.tipoActivo(),
				solicitud.moneda());
		return aRespuesta(guardar(activo));
	}

	@Transactional(readOnly = true)
	public List<ActivoRespuesta> listar() {
		return activoRepositorio.findAllByOrderBySimboloAsc().stream()
				.map(this::aRespuesta)
				.toList();
	}

	@Transactional(readOnly = true)
	public ActivoRespuesta obtener(Long id) {
		return aRespuesta(buscar(id));
	}

	/**
	 * Indica si el activo tiene transacciones, referidos, dividendos o precios (01-modelo-funcional §2.4).
	 * Esos módulos todavía no existen; cada spec que los agregue debe incorporar aquí su verificación.
	 */
	public boolean tieneRegistrosHistoricos(Long activoId) {
		return false;
	}

	private Activo buscar(Long id) {
		return activoRepositorio.findById(id)
				.orElseThrow(() -> new RecursoNoEncontradoExcepcion(ACTIVO_NO_ENCONTRADO,
						"No existe el activo solicitado."));
	}

	/**
	 * La verificación previa de unicidad no cubre dos solicitudes simultáneas;
	 * en ese caso la restricción de la base de datos se traduce al mismo error.
	 */
	private Activo guardar(Activo activo) {
		try {
			return activoRepositorio.saveAndFlush(activo);
		} catch (DataIntegrityViolationException ex) {
			if (ex.getCause() instanceof ConstraintViolationException violacion
					&& RESTRICCION_SIMBOLO_UNICO.equalsIgnoreCase(violacion.getConstraintName())) {
				throw simboloDuplicado(activo.getSimbolo());
			}
			throw ex;
		}
	}

	private ActivoRespuesta aRespuesta(Activo activo) {
		return ActivoRespuesta.desde(activo, !tieneRegistrosHistoricos(activo.getId()));
	}

	private static ConflictoExcepcion simboloDuplicado(String simbolo) {
		return new ConflictoExcepcion(SIMBOLO_DUPLICADO, "Ya existe un activo con el símbolo " + simbolo + ".",
				"simbolo");
	}

}
