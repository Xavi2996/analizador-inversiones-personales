package com.personal.inversiones.activos.servicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.personal.inversiones.activos.dto.ActivoRespuesta;
import com.personal.inversiones.activos.dto.ActualizarActivoSolicitud;
import com.personal.inversiones.activos.modelo.Activo;
import com.personal.inversiones.activos.modelo.Moneda;
import com.personal.inversiones.activos.modelo.TipoActivo;
import com.personal.inversiones.activos.repositorio.ActivoRepositorio;
import com.personal.inversiones.compartido.error.ConflictoExcepcion;

/**
 * Bloqueo del símbolo cuando el activo tiene registros históricos. Esos módulos todavía no existen,
 * así que {@code tieneRegistrosHistoricos} se simula en {@code true}.
 */
class ActivoServicioTest {

	private static final Long ID = 1L;

	private ActivoRepositorio activoRepositorio;
	private ActivoServicio activoServicio;

	@BeforeEach
	void preparar() {
		activoRepositorio = mock(ActivoRepositorio.class);
		activoServicio = spy(new ActivoServicio(activoRepositorio));
		// any(): el activo simulado no tiene id asignado, y la respuesta también consulta el bloqueo.
		doReturn(true).when(activoServicio).tieneRegistrosHistoricos(any());

		Activo activo = new Activo("IVV", "iShares Core S&P 500 ETF", TipoActivo.ETF, Moneda.USD);
		when(activoRepositorio.findById(ID)).thenReturn(Optional.of(activo));
		when(activoRepositorio.saveAndFlush(any(Activo.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
	}

	@Test
	void cambiarElSimboloConRegistrosHistoricosDevuelveSimboloBloqueado() {
		ActualizarActivoSolicitud solicitud = new ActualizarActivoSolicitud("VXUS", "iShares Core S&P 500 ETF",
				TipoActivo.ETF, Moneda.USD);

		assertThatThrownBy(() -> activoServicio.actualizar(ID, solicitud))
				.isInstanceOf(ConflictoExcepcion.class)
				.extracting(ex -> ((ConflictoExcepcion) ex).getCodigo())
				.isEqualTo(ActivoServicio.SIMBOLO_BLOQUEADO);
		verify(activoRepositorio, never()).saveAndFlush(any());
	}

	@Test
	void cambiarSoloLosDemasCamposConRegistrosHistoricosSeAcepta() {
		ActualizarActivoSolicitud solicitud = new ActualizarActivoSolicitud(" ivv ", "Nombre corregido",
				TipoActivo.ACCION, Moneda.USD);

		ActivoRespuesta respuesta = activoServicio.actualizar(ID, solicitud);

		assertThat(respuesta.simbolo()).isEqualTo("IVV");
		assertThat(respuesta.nombre()).isEqualTo("Nombre corregido");
		assertThat(respuesta.tipoActivo()).isEqualTo(TipoActivo.ACCION);
		assertThat(respuesta.simboloEditable()).isFalse();
	}

}
