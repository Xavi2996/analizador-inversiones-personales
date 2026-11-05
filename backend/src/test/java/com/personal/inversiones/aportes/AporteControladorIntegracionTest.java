package com.personal.inversiones.aportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.personal.inversiones.aportes.modelo.Aporte;
import com.personal.inversiones.aportes.repositorio.AporteRepositorio;
import com.personal.inversiones.compartido.PruebaIntegracion;

class AporteControladorIntegracionTest extends PruebaIntegracion {

	private static final String RUTA = "/api/v1/aportes";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AporteRepositorio aporteRepositorio;

	@Autowired
	private Clock clock;

	@Test
	void registrarCalculaElCostoDeFondeo() throws Exception {
		registrar("""
				{"fechaAporte": "2026-10-21", "montoAportado": 200.00, "montoNetoBroker": 195.58,
				 "metodoFondeo": "  Binance USDT → ERC20 → Hapi  ", "comentario": "Tercer aporte"}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.fechaAporte").value("2026-10-21"))
				.andExpect(jsonPath("$.montoAportado").value(200.0))
				.andExpect(jsonPath("$.montoNetoBroker").value(195.58))
				.andExpect(jsonPath("$.costoFondeo").value(4.42))
				.andExpect(jsonPath("$.porcentajeCostoFondeo").value(2.21))
				.andExpect(jsonPath("$.metodoFondeo").value("Binance USDT → ERC20 → Hapi"))
				.andExpect(jsonPath("$.comentario").value("Tercer aporte"));

		Aporte guardado = aporteRepositorio.findAll().getFirst();
		assertThat(guardado.getCostoFondeo()).isEqualByComparingTo("4.42");
		assertThat(guardado.getMetodoFondeo()).isEqualTo("Binance USDT → ERC20 → Hapi");
	}

	@Test
	void registrarDevuelveLosMontosConEscala2() throws Exception {
		registrar(aporte("2026-10-21", "200", "195.5"))
				.andExpect(status().isCreated())
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("\"montoAportado\":200.00", "\"montoNetoBroker\":195.50",
								"\"costoFondeo\":4.50"));
	}

	@Test
	void registrarConNetoIgualAlAportadoTieneCostoCero() throws Exception {
		registrar(aporte("2026-10-21", "100.00", "100.00"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.costoFondeo").value(0.0))
				.andExpect(jsonPath("$.porcentajeCostoFondeo").value(0.0));
	}

	@Test
	void registrarConNetoMayorAlAportadoDevuelve400() throws Exception {
		registrar(aporte("2026-10-21", "100.00", "100.01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos.length()").value(1))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("montoNetoBroker"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("No puede ser mayor al monto aportado."));

		assertThat(aporteRepositorio.count()).isZero();
	}

	@Test
	void registrarConMontoAportadoCeroONegativoDevuelve400() throws Exception {
		for (String monto : new String[] { "0", "-10.00" }) {
			registrar(aporte("2026-10-21", monto, "0"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
					.andExpect(jsonPath("$.erroresCampos[0].campo").value("montoAportado"))
					.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Debe ser mayor que cero."));
		}
		assertThat(aporteRepositorio.count()).isZero();
	}

	@Test
	void registrarConNetoNegativoDevuelve400() throws Exception {
		registrar(aporte("2026-10-21", "100.00", "-0.01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("montoNetoBroker"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("No puede ser negativo."));
	}

	@Test
	void registrarConTresDecimalesDevuelve400() throws Exception {
		registrar(aporte("2026-10-21", "100.001", "99.999"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[*].campo")
						.value(containsInAnyOrder("montoAportado", "montoNetoBroker")))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje")
						.value("Admite como máximo 2 decimales y 13 dígitos enteros."));
		assertThat(aporteRepositorio.count()).isZero();
	}

	@Test
	void registrarConMasDe13DigitosEnterosDevuelve400() throws Exception {
		registrar(aporte("2026-10-21", "12345678901234", "1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("montoAportado"));
	}

	@Test
	void registrarConFechaDeHoyEsValido() throws Exception {
		registrar(aporte(LocalDate.now(clock).toString(), "100.00", "98.00"))
				.andExpect(status().isCreated());
	}

	@Test
	void registrarConFechaFuturaDevuelve400() throws Exception {
		registrar(aporte(LocalDate.now(clock).plusDays(1).toString(), "100.00", "98.00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("fechaAporte"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("No puede ser una fecha futura."));
		assertThat(aporteRepositorio.count()).isZero();
	}

	@Test
	void registrarConFechaFuturaYNetoMayorInformaAmbosCampos() throws Exception {
		registrar(aporte(LocalDate.now(clock).plusDays(1).toString(), "100.00", "101.00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[*].campo")
						.value(containsInAnyOrder("fechaAporte", "montoNetoBroker")));
	}

	@Test
	void registrarSinCamposObligatoriosDevuelve400() throws Exception {
		registrar("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos.length()").value(3))
				.andExpect(jsonPath("$.erroresCampos[*].campo")
						.value(containsInAnyOrder("fechaAporte", "montoAportado", "montoNetoBroker")))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Es obligatorio."));
	}

	@Test
	void registrarConFechaMalFormadaDevuelve400() throws Exception {
		registrar(aporte("21/10/2026", "100.00", "98.00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("fechaAporte"));
	}

	@Test
	void registrarConMetodoDe51CaracteresDevuelve400() throws Exception {
		registrar(aporteConTextos("M".repeat(51), null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("metodoFondeo"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Admite como máximo 50 caracteres."));
	}

	@Test
	void registrarConMetodoDe50CaracteresEsValido() throws Exception {
		registrar(aporteConTextos("M".repeat(50), null)).andExpect(status().isCreated());
	}

	@Test
	void registrarConComentarioDe501CaracteresDevuelve400() throws Exception {
		registrar(aporteConTextos(null, "C".repeat(501)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("comentario"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Admite como máximo 500 caracteres."));
	}

	@Test
	void registrarConComentarioDe500CaracteresEsValido() throws Exception {
		registrar(aporteConTextos(null, "C".repeat(500))).andExpect(status().isCreated());
	}

	@Test
	void registrarGuardaTextosVaciosComoNull() throws Exception {
		registrar(aporteConTextos("   ", ""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.metodoFondeo").doesNotExist())
				.andExpect(jsonPath("$.comentario").doesNotExist());

		Aporte guardado = aporteRepositorio.findAll().getFirst();
		assertThat(guardado.getMetodoFondeo()).isNull();
		assertThat(guardado.getComentario()).isNull();
	}

	@Test
	void registrarIgnoraElCostoDeFondeoEnviado() throws Exception {
		registrar("""
				{"fechaAporte": "2026-10-13", "montoAportado": 125.00, "montoNetoBroker": 120.22,
				 "costoFondeo": 99.99}
				""")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.costoFondeo").value(4.78));

		assertThat(aporteRepositorio.findAll().getFirst().getCostoFondeo()).isEqualByComparingTo("4.78");
	}

	private ResultActions registrar(String json) throws Exception {
		return mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static String aporte(String fecha, String montoAportado, String montoNeto) {
		return """
				{"fechaAporte": "%s", "montoAportado": %s, "montoNetoBroker": %s}
				""".formatted(fecha, montoAportado, montoNeto);
	}

	private static String aporteConTextos(String metodo, String comentario) {
		return """
				{"fechaAporte": "2026-10-21", "montoAportado": 100.00, "montoNetoBroker": 98.00,
				 "metodoFondeo": %s, "comentario": %s}
				""".formatted(comoJson(metodo), comoJson(comentario));
	}

	private static String comoJson(String texto) {
		return texto == null ? "null" : "\"" + texto + "\"";
	}

}
