package com.personal.inversiones.aportes.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.personal.inversiones.aportes.dto.CrearAporteSolicitud;
import com.personal.inversiones.aportes.modelo.Aporte;
import com.personal.inversiones.aportes.repositorio.AporteRepositorio;
import com.personal.inversiones.compartido.error.DatosInvalidosExcepcion;
import com.personal.inversiones.compartido.error.ErrorCampo;

class AporteServicioTest {

	/** 2026-10-22 03:00 UTC = 2026-10-21 22:00 en Guayaquil (UTC-5). */
	private static final Clock RELOJ_FIJO = Clock.fixed(Instant.parse("2026-10-22T03:00:00Z"),
			ZoneId.of("America/Guayaquil"));

	private AporteRepositorio aporteRepositorio;
	private AporteServicio aporteServicio;

	@BeforeEach
	void preparar() {
		aporteRepositorio = mock(AporteRepositorio.class);
		when(aporteRepositorio.save(any(Aporte.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
		aporteServicio = new AporteServicio(aporteRepositorio, RELOJ_FIJO);
	}

	@Test
	void porcentajeRedondeaA2DecimalesHalfUp() {
		// 4.42 / 200 × 100 = 2.21 exacto
		assertThat(AporteServicio.porcentaje(new BigDecimal("4.42"), new BigDecimal("200"))).isEqualTo("2.21");
		// 6.67 / 250 × 100 = 2.668 → 2.67
		assertThat(AporteServicio.porcentaje(new BigDecimal("6.67"), new BigDecimal("250"))).isEqualTo("2.67");
		// 4.78 / 125 × 100 = 3.824 → 3.82
		assertThat(AporteServicio.porcentaje(new BigDecimal("4.78"), new BigDecimal("125"))).isEqualTo("3.82");
		// 0.125 exacto en la mitad: 1 / 800 × 100 = 0.125 → 0.13 (HALF_UP, no HALF_EVEN)
		assertThat(AporteServicio.porcentaje(new BigDecimal("1"), new BigDecimal("800"))).isEqualTo("0.13");
	}

	@Test
	void porcentajeConTotalCeroDevuelveCero() {
		assertThat(AporteServicio.porcentaje(BigDecimal.ZERO, BigDecimal.ZERO)).isEqualTo("0.00");
	}

	@Test
	void porcentajesTotalesDelExcel() {
		BigDecimal totalAportado = new BigDecimal("575.00");
		assertThat(AporteServicio.porcentaje(new BigDecimal("15.87"), totalAportado)).isEqualTo("2.76");
		assertThat(AporteServicio.porcentaje(new BigDecimal("559.13"), totalAportado)).isEqualTo("97.24");
	}

	@Test
	void montoSeDevuelveConEscala2() {
		assertThat(AporteServicio.monto(new BigDecimal("200.000000"))).isEqualTo("200.00");
		assertThat(AporteServicio.monto(new BigDecimal("195.5"))).isEqualTo("195.50");
	}

	@Test
	void aceptaLaFechaDeHoyEnGuayaquilAunqueEnUtcSeaManana() {
		var respuesta = aporteServicio.registrar(solicitud(LocalDate.parse("2026-10-21")));

		assertThat(respuesta.fechaAporte()).isEqualTo("2026-10-21");
		verify(aporteRepositorio).save(any(Aporte.class));
	}

	@Test
	void rechazaLaFechaDeMananaEnGuayaquilAunqueEnUtcSeaHoy() {
		DatosInvalidosExcepcion ex = catchThrowableOfType(DatosInvalidosExcepcion.class,
				() -> aporteServicio.registrar(solicitud(LocalDate.parse("2026-10-22"))));

		assertThat(ex.getErroresCampos())
				.containsExactly(new ErrorCampo("fechaAporte", "No puede ser una fecha futura."));
		verify(aporteRepositorio, never()).save(any());
	}

	private static CrearAporteSolicitud solicitud(LocalDate fecha) {
		return new CrearAporteSolicitud(fecha, new BigDecimal("200.00"), new BigDecimal("195.58"), null, null);
	}

}
