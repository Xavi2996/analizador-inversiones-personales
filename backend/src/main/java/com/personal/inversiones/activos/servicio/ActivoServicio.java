package com.personal.inversiones.activos.servicio;

import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.inversiones.activos.dto.ActivoRespuesta;
import com.personal.inversiones.activos.dto.ActualizarActivoSolicitud;
import com.personal.inversiones.activos.dto.CrearActivoSolicitud;
import com.personal.inversiones.activos.modelo.Activo;
import com.personal.inversiones.activos.repositorio.ActivoRepositorio;
import com.personal.inversiones.compartido.error.ConflictoExcepcion;
import com.personal.inversiones.compartido.error.RecursoNoEncontradoExcepcion;
import com.personal.inversiones.transacciones.repositorio.TransaccionRepositorio;

@Service
public class ActivoServicio {

	public static final String ACTIVO_NO_ENCONTRADO = "ACTIVO_NO_ENCONTRADO";
	public static final String SIMBOLO_DUPLICADO = "SIMBOLO_DUPLICADO";
	public static final String SIMBOLO_BLOQUEADO = "SIMBOLO_BLOQUEADO";

	private static final String RESTRICCION_SIMBOLO_UNICO = "uk_activo_simbolo";

	private final ActivoRepositorio activoRepositorio;
	private final TransaccionRepositorio transaccionRepositorio;

	public ActivoServicio(ActivoRepositorio activoRepositorio, TransaccionRepositorio transaccionRepositorio) {
		this.activoRepositorio = activoRepositorio;
		this.transaccionRepositorio = transaccionRepositorio;
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

	/**
	 * Enviar el mismo símbolo que ya tiene el activo no es un cambio y no se comprueba el bloqueo.
	 */
	@Transactional
	public ActivoRespuesta actualizar(Long id, ActualizarActivoSolicitud solicitud) {
		Activo activo = buscar(id);
		if (!solicitud.simbolo().equals(activo.getSimbolo())) {
			if (tieneRegistrosHistoricos(id)) {
				throw new ConflictoExcepcion(SIMBOLO_BLOQUEADO,
						"El símbolo no se puede modificar porque el activo tiene registros históricos.", "simbolo");
			}
			if (activoRepositorio.existsBySimbolo(solicitud.simbolo())) {
				throw simboloDuplicado(solicitud.simbolo());
			}
		}
		activo.actualizar(solicitud.simbolo(), solicitud.nombre(), solicitud.tipoActivo(), solicitud.moneda());
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
	 * Hoy solo existen las transacciones; las specs de Referidos, Dividendos y Precios deben incorporar
	 * aquí su verificación.
	 */
	public boolean tieneRegistrosHistoricos(Long activoId) {
		return transaccionRepositorio.existsByActivoId(activoId);
	}

	/**
	 * Lo usan también otros módulos que referencian un activo, para responder el mismo 404.
	 */
	public Activo buscar(Long id) {
		return activoRepositorio.findById(id).orElseThrow(ActivoServicio::activoNoEncontrado);
	}

	/**
	 * Igual que {@link #buscar(Long)}, pero bloquea la fila del activo hasta el fin de la transacción
	 * ({@code SELECT ... FOR UPDATE}). Lo usan las ventas para que dos registros simultáneos no superen
	 * la cantidad disponible (SPEC 003 §3.3). Debe llamarse dentro de una transacción.
	 */
	public Activo buscarConBloqueo(Long id) {
		return activoRepositorio.buscarConBloqueoPorId(id).orElseThrow(ActivoServicio::activoNoEncontrado);
	}

	private static RecursoNoEncontradoExcepcion activoNoEncontrado() {
		return new RecursoNoEncontradoExcepcion(ACTIVO_NO_ENCONTRADO, "No existe el activo solicitado.");
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
