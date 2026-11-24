package com.personal.inversiones.referidos.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.inversiones.activos.modelo.Activo;
import com.personal.inversiones.activos.servicio.ActivoServicio;
import com.personal.inversiones.compartido.error.DatosInvalidosExcepcion;
import com.personal.inversiones.compartido.error.ErrorCampo;
import com.personal.inversiones.compartido.error.RecursoNoEncontradoExcepcion;
import com.personal.inversiones.referidos.dto.CrearReferidoSolicitud;
import com.personal.inversiones.referidos.dto.ListaReferidosRespuesta;
import com.personal.inversiones.referidos.dto.ReferidoRespuesta;
import com.personal.inversiones.referidos.modelo.Referido;
import com.personal.inversiones.referidos.repositorio.ReferidoRepositorio;

@Service
public class ReferidoServicio {

	public static final String REFERIDO_NO_ENCONTRADO = "REFERIDO_NO_ENCONTRADO";

	/** Montos en USD: se devuelven con 2 decimales. */
	private static final int ESCALA_MONTO = 2;
	/** La entrada admite como máximo 8 decimales en la cantidad. */
	private static final int ESCALA_CANTIDAD = 8;
	/** El precio se devuelve tal como se guardó, con al menos 2 decimales. */
	private static final int ESCALA_MINIMA_PRECIO = 2;

	private final ReferidoRepositorio referidoRepositorio;
	private final ActivoServicio activoServicio;
	private final Clock clock;

	public ReferidoServicio(ReferidoRepositorio referidoRepositorio, ActivoServicio activoServicio, Clock clock) {
		this.referidoRepositorio = referidoRepositorio;
		this.activoServicio = activoServicio;
		this.clock = clock;
	}

	/**
	 * Orden de validación: datos de la solicitud (400) y activo existente (404).
	 * Un referido solo aumenta el saldo del activo: no necesita bloqueo (SPEC 004 §3.3).
	 */
	@Transactional
	public ReferidoRespuesta registrar(CrearReferidoSolicitud solicitud) {
		validarFecha(solicitud.fechaReferido());
		Activo activo = activoServicio.buscar(solicitud.activoId());
		BigDecimal valorRecibido = calcularValorRecibido(solicitud.cantidad(), solicitud.precioAlRecibir());
		Referido referido = new Referido(activo, solicitud.fechaReferido(), solicitud.origen(),
				solicitud.cantidad(), solicitud.precioAlRecibir(), valorRecibido, solicitud.comentario());
		return aRespuesta(referidoRepositorio.save(referido));
	}

	/** Historial por fecha e id descendentes, con el total del valor recibido y el conteo. */
	@Transactional(readOnly = true)
	public ListaReferidosRespuesta listar() {
		List<Referido> referidos = referidoRepositorio.listarConActivo();
		BigDecimal totalValorRecibido = referidos.stream()
				.map(Referido::getValorRecibido)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new ListaReferidosRespuesta(
				referidos.stream().map(ReferidoServicio::aRespuesta).toList(),
				totalValorRecibido.setScale(ESCALA_MONTO, RoundingMode.HALF_UP),
				referidos.size());
	}

	@Transactional(readOnly = true)
	public ReferidoRespuesta obtener(Long id) {
		return referidoRepositorio.buscarConActivo(id)
				.map(ReferidoServicio::aRespuesta)
				.orElseThrow(() -> new RecursoNoEncontradoExcepcion(REFERIDO_NO_ENCONTRADO,
						"No existe el referido solicitado."));
	}

	/**
	 * {@code cantidad × precioAlRecibir} redondeado a 2 decimales {@code HALF_UP}
	 * (01-modelo-funcional §5.3). Coincide con {@code ck_referido_valor_recibido}.
	 */
	static BigDecimal calcularValorRecibido(BigDecimal cantidad, BigDecimal precioAlRecibir) {
		return cantidad.multiply(precioAlRecibir).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
	}

	/** La fecha depende del reloj de negocio (01-modelo-funcional §17): Bean Validation no la cubre. */
	private void validarFecha(LocalDate fechaReferido) {
		if (fechaReferido.isAfter(LocalDate.now(clock))) {
			throw new DatosInvalidosExcepcion(
					List.of(new ErrorCampo("fechaReferido", "No puede ser una fecha futura.")));
		}
	}

	private static ReferidoRespuesta aRespuesta(Referido referido) {
		Activo activo = referido.getActivo();
		return new ReferidoRespuesta(
				referido.getId(),
				activo.getId(),
				activo.getSimbolo(),
				activo.getNombre(),
				referido.getFechaReferido(),
				referido.getOrigen(),
				referido.getCantidad().setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP),
				precio(referido.getPrecioAlRecibir()),
				referido.getValorRecibido().setScale(ESCALA_MONTO, RoundingMode.HALF_UP),
				referido.getComentario());
	}

	/** Quita los ceros finales de la columna ({@code 83580.270000 → 83580.27}) y conserva al menos 2 decimales. */
	private static BigDecimal precio(BigDecimal valor) {
		BigDecimal sinCeros = valor.stripTrailingZeros();
		return sinCeros.scale() < ESCALA_MINIMA_PRECIO ? sinCeros.setScale(ESCALA_MINIMA_PRECIO) : sinCeros;
	}

}
