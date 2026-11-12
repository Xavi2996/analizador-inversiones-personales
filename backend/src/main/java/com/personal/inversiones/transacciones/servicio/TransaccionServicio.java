package com.personal.inversiones.transacciones.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.inversiones.activos.modelo.Activo;
import com.personal.inversiones.activos.servicio.ActivoServicio;
import com.personal.inversiones.compartido.error.DatosInvalidosExcepcion;
import com.personal.inversiones.compartido.error.ErrorCampo;
import com.personal.inversiones.transacciones.dto.CrearTransaccionSolicitud;
import com.personal.inversiones.transacciones.dto.TransaccionRespuesta;
import com.personal.inversiones.transacciones.modelo.TipoTransaccion;
import com.personal.inversiones.transacciones.modelo.Transaccion;
import com.personal.inversiones.transacciones.repositorio.TransaccionRepositorio;

@Service
public class TransaccionServicio {

	/** Montos en USD: se devuelven con 2 decimales. */
	private static final int ESCALA_MONTO = 2;
	/** La entrada admite como máximo 8 decimales en la cantidad. */
	private static final int ESCALA_CANTIDAD = 8;
	/** El precio se devuelve tal como se guardó, con al menos 2 decimales. */
	private static final int ESCALA_MINIMA_PRECIO = 2;

	private final TransaccionRepositorio transaccionRepositorio;
	private final ActivoServicio activoServicio;
	private final Clock clock;

	public TransaccionServicio(TransaccionRepositorio transaccionRepositorio, ActivoServicio activoServicio,
			Clock clock) {
		this.transaccionRepositorio = transaccionRepositorio;
		this.activoServicio = activoServicio;
		this.clock = clock;
	}

	@Transactional
	public TransaccionRespuesta registrar(CrearTransaccionSolicitud solicitud) {
		validar(solicitud);
		Activo activo = activoServicio.buscar(solicitud.activoId());
		BigDecimal montoBruto = calcularMontoBruto(solicitud.cantidad(), solicitud.precioUnitario());
		Transaccion transaccion = new Transaccion(activo, solicitud.tipoTransaccion(), solicitud.fechaTransaccion(),
				solicitud.cantidad(), solicitud.precioUnitario(), montoBruto, solicitud.comision(),
				solicitud.comentario());
		return aRespuesta(transaccionRepositorio.save(transaccion));
	}

	/**
	 * {@code cantidad × precioUnitario} redondeado a 2 decimales {@code HALF_UP}
	 * (01-modelo-funcional §4.3). Coincide con {@code ck_transaccion_monto_bruto}.
	 */
	static BigDecimal calcularMontoBruto(BigDecimal cantidad, BigDecimal precioUnitario) {
		return cantidad.multiply(precioUnitario).setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
	}

	/**
	 * Con signo (01-modelo-funcional §4.6): compra −(monto + comisión); venta monto − comisión.
	 */
	static BigDecimal calcularEfectivoNeto(TipoTransaccion tipo, BigDecimal montoBruto, BigDecimal comision) {
		return tipo == TipoTransaccion.COMPRA
				? montoBruto.add(comision).negate()
				: montoBruto.subtract(comision);
	}

	/**
	 * Reglas que Bean Validation no cubre: la fecha depende del reloj de negocio (01-modelo-funcional §17).
	 */
	private void validar(CrearTransaccionSolicitud solicitud) {
		List<ErrorCampo> errores = new ArrayList<>();
		if (solicitud.fechaTransaccion().isAfter(LocalDate.now(clock))) {
			errores.add(new ErrorCampo("fechaTransaccion", "No puede ser una fecha futura."));
		}
		if (!errores.isEmpty()) {
			throw new DatosInvalidosExcepcion(errores);
		}
	}

	private TransaccionRespuesta aRespuesta(Transaccion transaccion) {
		Activo activo = transaccion.getActivo();
		return new TransaccionRespuesta(
				transaccion.getId(),
				activo.getId(),
				activo.getSimbolo(),
				activo.getNombre(),
				transaccion.getTipoTransaccion(),
				transaccion.getFechaTransaccion(),
				cantidad(transaccion.getCantidad()),
				precio(transaccion.getPrecioUnitario()),
				monto(transaccion.getMontoBruto()),
				monto(transaccion.getComision()),
				monto(calcularEfectivoNeto(transaccion.getTipoTransaccion(), transaccion.getMontoBruto(),
						transaccion.getComision())),
				transaccion.getComentario());
	}

	/** El monto bruto ya está redondeado y la comisión admite 2 decimales: cambiar la escala no redondea. */
	static BigDecimal monto(BigDecimal valor) {
		return valor.setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
	}

	/** La entrada admite como máximo 8 decimales: cambiar la escala no redondea. */
	static BigDecimal cantidad(BigDecimal valor) {
		return valor.setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP);
	}

	/** Quita los ceros finales de la columna ({@code 769.570000 → 769.57}) y conserva al menos 2 decimales. */
	static BigDecimal precio(BigDecimal valor) {
		BigDecimal sinCeros = valor.stripTrailingZeros();
		return sinCeros.scale() < ESCALA_MINIMA_PRECIO ? sinCeros.setScale(ESCALA_MINIMA_PRECIO) : sinCeros;
	}

}
