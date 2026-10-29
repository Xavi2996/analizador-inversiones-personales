package com.personal.inversiones.activos;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.personal.inversiones.activos.repositorio.ActivoRepositorio;
import com.personal.inversiones.compartido.PruebaIntegracion;

class ActivoControladorIntegracionTest extends PruebaIntegracion {

	private static final String RUTA = "/api/v1/activos";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ActivoRepositorio activoRepositorio;

	@Test
	void crearDevuelve201ConElActivoNormalizado() throws Exception {
		crear(" ivv ", " iShares Core S&P 500 ETF ", "ETF", "USD")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.simbolo").value("IVV"))
				.andExpect(jsonPath("$.nombre").value("iShares Core S&P 500 ETF"))
				.andExpect(jsonPath("$.tipoActivo").value("ETF"))
				.andExpect(jsonPath("$.moneda").value("USD"))
				.andExpect(jsonPath("$.simboloEditable").value(true))
				.andExpect(jsonPath("$.fechaCreacion").doesNotExist())
				.andExpect(jsonPath("$.fechaActualizacion").doesNotExist());
	}

	@Test
	void crearConSimboloExistenteDevuelve409() throws Exception {
		crear(" ivv ", "iShares Core S&P 500 ETF", "ETF", "USD").andExpect(status().isCreated());

		crear("IVV", "Otro nombre", "ETF", "USD")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("SIMBOLO_DUPLICADO"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("simbolo"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Ya existe un activo con el símbolo IVV."));
	}

	@Test
	void crearSinNombreDevuelve400() throws Exception {
		mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"simbolo":"IVV","tipoActivo":"ETF","moneda":"USD"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos[*].campo").value(hasItem("nombre")));
	}

	@Test
	void crearSinCamposDevuelve400ConTodosLosCampos() throws Exception {
		mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos.length()").value(4));
	}

	@Test
	void crearConTipoInvalidoDevuelve400() throws Exception {
		crear("BTC", "Bitcoin", "CRIPTO", "USD")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("tipoActivo"));
	}

	@Test
	void crearConMonedaInvalidaDevuelve400() throws Exception {
		crear("IVV", "iShares Core S&P 500 ETF", "ETF", "EUR")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("moneda"));
	}

	@Test
	void crearConSimboloConEspacioInternoDevuelve400() throws Exception {
		crear("IV V", "iShares Core S&P 500 ETF", "ETF", "USD")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("simbolo"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje")
						.value("Solo se permiten letras, números, punto y guion (máximo 20)."));
	}

	@Test
	void crearConSimboloDeMasDe20CaracteresDevuelve400() throws Exception {
		crear("A".repeat(21), "Nombre", "ACCION", "USD")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("simbolo"));
	}

	@Test
	void crearConSimboloVacioDevuelveUnSoloError() throws Exception {
		crear("   ", "Nombre", "ACCION", "USD")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos.length()").value(1))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Es obligatorio."));
	}

	@Test
	void crearConJsonMalFormadoDevuelve400SinDetallesInternos() throws Exception {
		mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"simbolo\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(content().string(not(containsString("Exception"))))
				.andExpect(content().string(not(containsString("jackson"))));
	}

	@Test
	void listarDevuelveLosActivosOrdenadosPorSimbolo() throws Exception {
		crear("VXUS", "Vanguard Total International Stock ETF", "ETF", "USD");
		crear("BTC", "Bitcoin", "CRIPTOMONEDA", "USD");
		crear("IVV", "iShares Core S&P 500 ETF", "ETF", "USD");

		mockMvc.perform(get(RUTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].simbolo").value("BTC"))
				.andExpect(jsonPath("$[1].simbolo").value("IVV"))
				.andExpect(jsonPath("$[2].simbolo").value("VXUS"))
				.andExpect(jsonPath("$[0].simboloEditable").value(true));
	}

	@Test
	void listarSinActivosDevuelveListaVacia() throws Exception {
		mockMvc.perform(get(RUTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(0));
	}

	@Test
	void obtenerDevuelveElActivo() throws Exception {
		Long id = crearYObtenerId("IVV");

		mockMvc.perform(get(RUTA + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.simbolo").value("IVV"))
				.andExpect(jsonPath("$.simboloEditable").value(true));
	}

	@Test
	void obtenerInexistenteDevuelve404() throws Exception {
		mockMvc.perform(get(RUTA + "/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("ACTIVO_NO_ENCONTRADO"))
				.andExpect(jsonPath("$.ruta").value(RUTA + "/999"));
	}

	@Test
	void actualizarModificaLosCuatroCampos() throws Exception {
		Long id = crearYObtenerId("IVV");

		actualizar(id, " vxus ", " Vanguard Total International Stock ETF ", "ACCION", "USD")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.simbolo").value("VXUS"))
				.andExpect(jsonPath("$.nombre").value("Vanguard Total International Stock ETF"))
				.andExpect(jsonPath("$.tipoActivo").value("ACCION"))
				.andExpect(jsonPath("$.moneda").value("USD"))
				.andExpect(jsonPath("$.simboloEditable").value(true))
				.andExpect(jsonPath("$.fechaActualizacion").doesNotExist());

		mockMvc.perform(get(RUTA + "/" + id))
				.andExpect(jsonPath("$.simbolo").value("VXUS"))
				.andExpect(jsonPath("$.tipoActivo").value("ACCION"));
	}

	@Test
	void actualizarConSimboloDeOtroActivoDevuelve409() throws Exception {
		crear("IVV", "iShares Core S&P 500 ETF", "ETF", "USD").andExpect(status().isCreated());
		crear("VXUS", "Vanguard Total International Stock ETF", "ETF", "USD").andExpect(status().isCreated());
		Long idVxus = activoRepositorio.findAllByOrderBySimboloAsc().get(1).getId();

		actualizar(idVxus, "ivv", "Vanguard Total International Stock ETF", "ETF", "USD")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("SIMBOLO_DUPLICADO"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("simbolo"));
	}

	@Test
	void actualizarConElMismoSimboloDevuelve200() throws Exception {
		Long id = crearYObtenerId("IVV");

		actualizar(id, "IVV", "Nombre corregido", "ETF", "USD")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.simbolo").value("IVV"))
				.andExpect(jsonPath("$.nombre").value("Nombre corregido"));
	}

	@Test
	void actualizarInexistenteDevuelve404() throws Exception {
		actualizar(999L, "IVV", "iShares Core S&P 500 ETF", "ETF", "USD")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("ACTIVO_NO_ENCONTRADO"));
	}

	@Test
	void actualizarConDatosInvalidosDevuelve400() throws Exception {
		Long id = crearYObtenerId("IVV");

		actualizar(id, "IV V", "", "ETF", "EUR")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));

		mockMvc.perform(put(RUTA + "/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"simbolo":"IV V","nombre":"","tipoActivo":"ETF","moneda":"USD"}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[*].campo").value(hasItem("simbolo")))
				.andExpect(jsonPath("$.erroresCampos[*].campo").value(hasItem("nombre")));
	}

	private ResultActions actualizar(Long id, String simbolo, String nombre, String tipoActivo, String moneda)
			throws Exception {
		String cuerpo = """
				{"simbolo":"%s","nombre":"%s","tipoActivo":"%s","moneda":"%s"}
				""".formatted(simbolo, nombre, tipoActivo, moneda);
		return mockMvc.perform(put(RUTA + "/" + id).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	private ResultActions crear(String simbolo, String nombre, String tipoActivo, String moneda) throws Exception {
		String cuerpo = """
				{"simbolo":"%s","nombre":"%s","tipoActivo":"%s","moneda":"%s"}
				""".formatted(simbolo, nombre, tipoActivo, moneda);
		return mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
	}

	private Long crearYObtenerId(String simbolo) throws Exception {
		crear(simbolo, "iShares Core S&P 500 ETF", "ETF", "USD").andExpect(status().isCreated());
		return activoRepositorio.findAll().getFirst().getId();
	}

}
