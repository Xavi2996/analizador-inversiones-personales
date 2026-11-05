package com.personal.inversiones.aportes.servicio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.personal.inversiones.aportes.dto.AporteRespuesta;
import com.personal.inversiones.aportes.dto.CrearAporteSolicitud;
import com.personal.inversiones.aportes.modelo.Aporte;
import com.personal.inversiones.aportes.repositorio.AporteRepositorio;
import com.personal.inversiones.compartido.error.DatosInvalidosExcepcion;
import com.personal.inversiones.compartido.error.ErrorCampo;

@Service
public class AporteServicio {

	/** Montos en USD: se devuelven con 2 decimales. */
	private static final int ESCALA_MONTO = 2;
	private static final int ESCALA_PORCENTAJE = 2;
	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private final AporteRepositorio aporteRepositorio;
	private final Clock clock;

	public AporteServicio(AporteRepositorio aporteRepositorio, Clock clock) {
		this.aporteRepositorio = aporteRepositorio;
		this.clock = clock;
	}

	@Transactional
	public AporteRespuesta registrar(CrearAporteSolicitud solicitud) {
		validar(solicitud);
		BigDecimal costoFondeo = solicitud.montoAportado().subtract(solicitud.montoNetoBroker());
		Aporte aporte = new Aporte(solicitud.fechaAporte(), solicitud.montoAportado(), solicitud.montoNetoBroker(),
				costoFondeo, solicitud.metodoFondeo(), solicitud.comentario());
		return aRespuesta(aporteRepositorio.save(aporte));
	}

	/**
	 * Reglas que Bean Validation no cubre: la fecha depende del reloj de negocio (01-modelo-funcional §17)
	 * y el neto se compara con el monto aportado.
	 */
	private void validar(CrearAporteSolicitud solicitud) {
		List<ErrorCampo> errores = new ArrayList<>();
		if (solicitud.fechaAporte().isAfter(LocalDate.now(clock))) {
			errores.add(new ErrorCampo("fechaAporte", "No puede ser una fecha futura."));
		}
		if (solicitud.montoNetoBroker().compareTo(solicitud.montoAportado()) > 0) {
			errores.add(new ErrorCampo("montoNetoBroker", "No puede ser mayor al monto aportado."));
		}
		if (!errores.isEmpty()) {
			throw new DatosInvalidosExcepcion(errores);
		}
	}

	private AporteRespuesta aRespuesta(Aporte aporte) {
		return new AporteRespuesta(
				aporte.getId(),
				aporte.getFechaAporte(),
				monto(aporte.getMontoAportado()),
				monto(aporte.getMontoNetoBroker()),
				monto(aporte.getCostoFondeo()),
				porcentaje(aporte.getCostoFondeo(), aporte.getMontoAportado()),
				aporte.getMetodoFondeo(),
				aporte.getComentario());
	}

	/** La entrada admite como máximo 2 decimales, así que cambiar la escala no redondea. */
	static BigDecimal monto(BigDecimal valor) {
		return valor.setScale(ESCALA_MONTO, RoundingMode.HALF_UP);
	}

	/** {@code parte / total × 100} con 2 decimales HALF_UP; 0 si el total es cero. */
	static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
		if (total.signum() == 0) {
			return BigDecimal.ZERO.setScale(ESCALA_PORCENTAJE);
		}
		return parte.multiply(CIEN).divide(total, ESCALA_PORCENTAJE, RoundingMode.HALF_UP);
	}

}
