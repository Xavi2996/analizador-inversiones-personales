package com.personal.inversiones.asignaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.personal.inversiones.activos.modelo.Activo;
import com.personal.inversiones.activos.modelo.Moneda;
import com.personal.inversiones.activos.modelo.TipoActivo;
import com.personal.inversiones.activos.repositorio.ActivoRepositorio;
import com.personal.inversiones.asignaciones.repositorio.AsignacionObjetivoRepositorio;
import com.personal.inversiones.compartido.PruebaIntegracion;

class AsignacionObjetivoControladorIntegracionTest extends PruebaIntegracion {

	private static final String RUTA = "/api/v1/asignaciones";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ActivoRepositorio activoRepositorio;

	@Autowired
	private AsignacionObjetivoRepositorio asignacionRepositorio;

	@Test
	void guardarCreaLaAsignacion() throws Exception {
		Long id = crearActivo("IVV", TipoActivo.ETF);

		guardar(id, "85.00")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activoId").value(id))
				.andExpect(jsonPath("$.simbolo").value("IVV"))
				.andExpect(jsonPath("$.porcentajeObjetivo").value(85.0));

		assertThat(asignacionRepositorio.findByActivoId(id)).get()
				.extracting(a -> a.getPorcentajeObjetivo())
				.satisfies(p -> assertThat(p).isEqualByComparingTo("85"));
	}

	@Test
	void guardarDeNuevoActualizaSinDuplicarLaFila() throws Exception {
		Long id = crearActivo("IVV", TipoActivo.ETF);

		guardar(id, "85").andExpect(status().isOk());
		guardar(id, "80")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.porcentajeObjetivo").value(80.0));

		assertThat(asignacionRepositorio.count()).isEqualTo(1);
		assertThat(asignacionRepositorio.findByActivoId(id).orElseThrow().getPorcentajeObjetivo())
				.isEqualByComparingTo("80");
	}

	@Test
	void guardarAceptaLosLimites0Y100() throws Exception {
		guardar(crearActivo("IVV", TipoActivo.ETF), "0").andExpect(status().isOk());
		guardar(crearActivo("VXUS", TipoActivo.ETF), "100").andExpect(status().isOk());
	}

	@Test
	void guardarFueraDeRangoDevuelve400() throws Exception {
		Long id = crearActivo("IVV", TipoActivo.ETF);

		guardar(id, "-1")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("porcentajeObjetivo"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Debe estar entre 0 y 100."));
		guardar(id, "100.01")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));

		assertThat(asignacionRepositorio.count()).isZero();
	}

	@Test
	void guardarConMasDe4DecimalesDevuelve400() throws Exception {
		guardar(crearActivo("IVV", TipoActivo.ETF), "10.12345")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Admite como máximo 4 decimales."));
	}

	@Test
	void guardarSinPorcentajeDevuelve400() throws Exception {
		mockMvc.perform(put(RUTA + "/" + crearActivo("IVV", TipoActivo.ETF))
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("porcentajeObjetivo"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Es obligatorio."));
	}

	@Test
	void guardarConActivoInexistenteDevuelve404() throws Exception {
		guardar(999L, "85")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("ACTIVO_NO_ENCONTRADO"));
	}

	@Test
	void listarConSuma100IndicaSumaCorrecta() throws Exception {
		guardar(crearActivo("IVV", TipoActivo.ETF), "85");
		guardar(crearActivo("VXUS", TipoActivo.ETF), "10");
		guardar(crearActivo("BTC", TipoActivo.CRIPTOMONEDA), "5");

		mockMvc.perform(get(RUTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.asignaciones.length()").value(3))
				.andExpect(jsonPath("$.asignaciones[0].simbolo").value("BTC"))
				.andExpect(jsonPath("$.asignaciones[1].simbolo").value("IVV"))
				.andExpect(jsonPath("$.asignaciones[2].simbolo").value("VXUS"))
				.andExpect(jsonPath("$.asignaciones[0].porcentajeObjetivo").value(5.0))
				.andExpect(jsonPath("$.sumaPorcentajes").value(100.0))
				.andExpect(jsonPath("$.sumaCorrecta").value(true));
	}

	@Test
	void listarConSuma95IndicaSumaIncorrecta() throws Exception {
		guardar(crearActivo("IVV", TipoActivo.ETF), "85");
		guardar(crearActivo("VXUS", TipoActivo.ETF), "10");
		crearActivo("BTC", TipoActivo.CRIPTOMONEDA);

		mockMvc.perform(get(RUTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.asignaciones.length()").value(2))
				.andExpect(jsonPath("$.sumaPorcentajes").value(95.0))
				.andExpect(jsonPath("$.sumaCorrecta").value(false));
	}

	@Test
	void listarConDecimalesSumaExactamente100() throws Exception {
		guardar(crearActivo("IVV", TipoActivo.ETF), "33.3333");
		guardar(crearActivo("VXUS", TipoActivo.ETF), "33.3333");
		guardar(crearActivo("BTC", TipoActivo.CRIPTOMONEDA), "33.3334");

		mockMvc.perform(get(RUTA))
				.andExpect(jsonPath("$.sumaPorcentajes").value(100.0))
				.andExpect(jsonPath("$.sumaCorrecta").value(true));
	}

	@Test
	void listarSinAsignacionesDevuelveSuma0() throws Exception {
		crearActivo("IVV", TipoActivo.ETF);

		mockMvc.perform(get(RUTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.asignaciones.length()").value(0))
				.andExpect(jsonPath("$.sumaPorcentajes").value(0.0))
				.andExpect(jsonPath("$.sumaCorrecta").value(false));
	}

	private Long crearActivo(String simbolo, TipoActivo tipo) {
		return activoRepositorio.save(new Activo(simbolo, "Nombre de " + simbolo, tipo, Moneda.USD)).getId();
	}

	private ResultActions guardar(Long activoId, String porcentaje) throws Exception {
		return mockMvc.perform(put(RUTA + "/" + activoId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"porcentajeObjetivo\":" + new BigDecimal(porcentaje).toPlainString() + "}"));
	}

}
