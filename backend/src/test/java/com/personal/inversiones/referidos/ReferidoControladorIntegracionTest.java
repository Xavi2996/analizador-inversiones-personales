package com.personal.inversiones.referidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.personal.inversiones.activos.modelo.Activo;
import com.personal.inversiones.activos.modelo.Moneda;
import com.personal.inversiones.activos.modelo.TipoActivo;
import com.personal.inversiones.activos.repositorio.ActivoRepositorio;
import com.personal.inversiones.compartido.PruebaIntegracion;
import com.personal.inversiones.referidos.modelo.Referido;
import com.personal.inversiones.referidos.repositorio.ReferidoRepositorio;

class ReferidoControladorIntegracionTest extends PruebaIntegracion {

	private static final String RUTA = "/api/v1/referidos";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ActivoRepositorio activoRepositorio;

	@Autowired
	private ReferidoRepositorio referidoRepositorio;

	@Autowired
	private Clock clock;

	private Long btc;

	@BeforeEach
	void crearActivo() {
		btc = activoRepositorio.save(new Activo("BTC", "Bitcoin", TipoActivo.CRIPTOMONEDA, Moneda.USD)).getId();
	}

	// --- Cálculo del valor recibido (SPEC 004 §3.2) ---

	@Test
	void registrarCalculaElValorRecibido() throws Exception {
		registrar("""
				{"activoId": %d, "fechaReferido": "2026-10-13", "origen": "  Programa de referidos Hapi  ",
				 "cantidad": 0.00001196, "precioAlRecibir": 83580.27, "comentario": "  Premio Hapi  "}
				""".formatted(btc))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.activoId").value(btc))
				.andExpect(jsonPath("$.simbolo").value("BTC"))
				.andExpect(jsonPath("$.nombreActivo").value("Bitcoin"))
				.andExpect(jsonPath("$.fechaReferido").value("2026-10-13"))
				.andExpect(jsonPath("$.origen").value("Programa de referidos Hapi"))
				.andExpect(jsonPath("$.cantidad").value(0.00001196))
				.andExpect(jsonPath("$.precioAlRecibir").value(83580.27))
				.andExpect(jsonPath("$.valorRecibido").value(1.0))
				.andExpect(jsonPath("$.comentario").value("Premio Hapi"))
				.andExpect(jsonPath("$.fechaCreacion").doesNotExist())
				.andExpect(jsonPath("$.fechaActualizacion").doesNotExist());

		Referido guardado = referidoRepositorio.findAll().getFirst();
		assertThat(guardado.getValorRecibido()).isEqualByComparingTo("1.00");
		assertThat(guardado.getOrigen()).isEqualTo("Programa de referidos Hapi");
	}

	@Test
	void registrarDevuelveLasEscalasDelContrato() throws Exception {
		registrar(referido("0.00001196", "83580.27"))
				.andExpect(status().isCreated())
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("\"cantidad\":0.00001196", "\"precioAlRecibir\":83580.27",
								"\"valorRecibido\":1.00"));
	}

	@Test
	void registrarDevuelvePrecioEnteroConDosDecimales() throws Exception {
		registrar(referido("0.5", "100"))
				.andExpect(status().isCreated())
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("\"cantidad\":0.50000000", "\"precioAlRecibir\":100.00",
								"\"valorRecibido\":50.00"));
	}

	@Test
	void valorRecibidoPuedeSerCero() throws Exception {
		// 0.00000001 × 100 = 0.000001 → 0.00
		registrar(referido("0.00000001", "100.00"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.valorRecibido").value(0.0));
	}

	@Test
	void valorRecibidoRedondeaLaMitadHaciaArriba() throws Exception {
		// 0.5 × 0.01 = 0.005 → 0.01. Debe coincidir con ck_referido_valor_recibido (ROUND de PostgreSQL).
		registrar(referido("0.5", "0.01"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.valorRecibido").value(0.01));
	}

	@Test
	void registrarIgnoraElValorRecibidoEnviado() throws Exception {
		registrar("""
				{"activoId": %d, "fechaReferido": "2026-10-13", "origen": "Hapi",
				 "cantidad": 0.00001196, "precioAlRecibir": 83580.27, "valorRecibido": 999.99}
				""".formatted(btc))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.valorRecibido").value(1.0));

		assertThat(referidoRepositorio.findAll().getFirst().getValorRecibido()).isEqualByComparingTo("1.00");
	}

	// --- Validaciones de campos ---

	@Test
	void registrarConCantidadOPrecioCeroONegativoDevuelve400() throws Exception {
		for (String valor : new String[] { "0", "-1" }) {
			registrar(referido(valor, "100"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
					.andExpect(jsonPath("$.erroresCampos[0].campo").value("cantidad"))
					.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Debe ser mayor que cero."));
			registrar(referido("1", valor))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.erroresCampos[0].campo").value("precioAlRecibir"))
					.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Debe ser mayor que cero."));
		}
		assertThat(referidoRepositorio.count()).isZero();
	}

	@Test
	void registrarConExcesoDeDecimalesDevuelve400() throws Exception {
		registrar(referido("0.123456789", "100.1234567"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos.length()").value(2))
				.andExpect(jsonPath("$.erroresCampos[*].campo")
						.value(containsInAnyOrder("cantidad", "precioAlRecibir")));
		assertThat(referidoRepositorio.count()).isZero();
	}

	@Test
	void registrarConElMaximoDeDecimalesEsValido() throws Exception {
		registrar(referido("0.12345678", "100.123456")).andExpect(status().isCreated());
	}

	@Test
	void registrarConFechaDeHoyEsValido() throws Exception {
		registrar(referido(btc, LocalDate.now(clock).toString(), "\"Hapi\"", "1", "100"))
				.andExpect(status().isCreated());
	}

	@Test
	void registrarConFechaFuturaDevuelve400() throws Exception {
		registrar(referido(btc, LocalDate.now(clock).plusDays(1).toString(), "\"Hapi\"", "1", "100"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("fechaReferido"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("No puede ser una fecha futura."));
		assertThat(referidoRepositorio.count()).isZero();
	}

	@Test
	void registrarSinCamposObligatoriosDevuelve400() throws Exception {
		registrar("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos.length()").value(5))
				.andExpect(jsonPath("$.erroresCampos[*].campo").value(containsInAnyOrder("activoId",
						"fechaReferido", "origen", "cantidad", "precioAlRecibir")))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Es obligatorio."));
	}

	@Test
	void registrarConOrigenEnBlancoDevuelve400() throws Exception {
		registrar(referido(btc, "2026-10-13", "\"   \"", "1", "100"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("origen"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Es obligatorio."));
		assertThat(referidoRepositorio.count()).isZero();
	}

	@Test
	void registrarConOrigenDe101CaracteresDevuelve400() throws Exception {
		registrar(referido(btc, "2026-10-13", "\"" + "O".repeat(101) + "\"", "1", "100"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("origen"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Admite como máximo 100 caracteres."));
	}

	@Test
	void registrarConOrigenDe100CaracteresEsValido() throws Exception {
		registrar(referido(btc, "2026-10-13", "\"" + "O".repeat(100) + "\"", "1", "100"))
				.andExpect(status().isCreated());
	}

	@Test
	void registrarConActivoInexistenteDevuelve404() throws Exception {
		registrar(referido(999L, "2026-10-13", "\"Hapi\"", "1", "100"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("ACTIVO_NO_ENCONTRADO"));
		assertThat(referidoRepositorio.count()).isZero();
	}

	@Test
	void registrarAceptaCualquierTipoDeActivo() throws Exception {
		Long ivv = activoRepositorio.save(new Activo("IVV", "iShares Core S&P 500 ETF", TipoActivo.ETF, Moneda.USD))
				.getId();
		registrar(referido(ivv, "2026-10-13", "\"Programa de acciones\"", "0.01", "770"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.simbolo").value("IVV"));
	}

	@Test
	void registrarConComentarioDe501CaracteresDevuelve400() throws Exception {
		registrar(referidoConComentario("\"" + "C".repeat(501) + "\""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("comentario"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Admite como máximo 500 caracteres."));
	}

	@Test
	void registrarConComentarioDe500CaracteresEsValido() throws Exception {
		registrar(referidoConComentario("\"" + "C".repeat(500) + "\"")).andExpect(status().isCreated());
	}

	@Test
	void registrarGuardaComentarioVacioComoNull() throws Exception {
		registrar(referidoConComentario("\"   \""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.comentario").doesNotExist());
		assertThat(referidoRepositorio.findAll().getFirst().getComentario()).isNull();
	}

	@Test
	void registrarConJsonMalFormadoDevuelve400() throws Exception {
		registrar("{\"activoId\": ")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
	}

	// --- Consultar (SPEC 004 §3.5) ---

	@Test
	void listarSinReferidosDevuelveListaVaciaYCeros() throws Exception {
		mockMvc.perform(get(RUTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.referidos").isEmpty())
				.andExpect(jsonPath("$.totalValorRecibido").value(0.0))
				.andExpect(jsonPath("$.conteo").value(0))
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("\"totalValorRecibido\":0.00"));
	}

	@Test
	void listarOrdenaPorFechaEIdDescendentesYCalculaTotales() throws Exception {
		Long primero = idRegistrado(referido(btc, "2026-10-10", "\"A\"", "0.5", "1.00"));
		Long tercero = idRegistrado(referido(btc, "2026-10-13", "\"B\"", "0.00001196", "83580.27"));
		Long segundo = idRegistrado(referido(btc, "2026-10-10", "\"C\"", "1", "2.50"));

		mockMvc.perform(get(RUTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.referidos[*].id").value(contains(tercero.intValue(), segundo.intValue(),
						primero.intValue())))
				.andExpect(jsonPath("$.referidos[0].simbolo").value("BTC"))
				.andExpect(jsonPath("$.referidos[0].valorRecibido").value(1.0))
				.andExpect(jsonPath("$.referidos[0].fechaCreacion").doesNotExist())
				// 0.50 + 1.00 + 2.50
				.andExpect(jsonPath("$.totalValorRecibido").value(4.0))
				.andExpect(jsonPath("$.conteo").value(3));
	}

	@Test
	void obtenerDevuelveElReferido() throws Exception {
		Long id = idRegistrado(referido("0.00001196", "83580.27"));

		mockMvc.perform(get(RUTA + "/" + id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.simbolo").value("BTC"))
				.andExpect(jsonPath("$.nombreActivo").value("Bitcoin"))
				.andExpect(jsonPath("$.origen").value("Programa de referidos Hapi"))
				.andExpect(jsonPath("$.valorRecibido").value(1.0))
				.andExpect(jsonPath("$.fechaCreacion").doesNotExist());
	}

	@Test
	void obtenerInexistenteDevuelve404() throws Exception {
		mockMvc.perform(get(RUTA + "/999"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("REFERIDO_NO_ENCONTRADO"))
				.andExpect(jsonPath("$.ruta").value(RUTA + "/999"));
	}

	@Test
	void editarOEliminarNoEstanPermitidos() throws Exception {
		Long id = idRegistrado(referido("1", "100"));

		mockMvc.perform(put(RUTA + "/" + id).contentType(MediaType.APPLICATION_JSON).content(referido("2", "100")))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
		mockMvc.perform(delete(RUTA + "/" + id))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));

		assertThat(referidoRepositorio.count()).isEqualTo(1);
		assertThat(referidoRepositorio.findAll().getFirst().getCantidad()).isEqualByComparingTo("1");
	}

	// --- Bloqueo del símbolo del activo (01-modelo-funcional §2.4) ---

	@Test
	void activoConReferidoYSinTransaccionesBloqueaElSimbolo() throws Exception {
		mockMvc.perform(get("/api/v1/activos/" + btc))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.simboloEditable").value(true));

		registrar(referido("0.00001196", "83580.27")).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/activos/" + btc))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.simboloEditable").value(false));
		actualizarActivo("BTCX", "CRIPTOMONEDA")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("SIMBOLO_BLOQUEADO"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("simbolo"));

		assertThat(activoRepositorio.findById(btc).orElseThrow().getSimbolo()).isEqualTo("BTC");
	}

	@Test
	void activoConReferidoPermiteCambiarElTipo() throws Exception {
		registrar(referido("0.00001196", "83580.27")).andExpect(status().isCreated());

		actualizarActivo("BTC", "ETF")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.simbolo").value("BTC"))
				.andExpect(jsonPath("$.tipoActivo").value("ETF"));
	}

	// --- Utilidades ---

	private ResultActions actualizarActivo(String simbolo, String tipoActivo) throws Exception {
		return mockMvc.perform(put("/api/v1/activos/" + btc).contentType(MediaType.APPLICATION_JSON).content("""
				{"simbolo": "%s", "nombre": "Bitcoin", "tipoActivo": "%s", "moneda": "USD"}
				""".formatted(simbolo, tipoActivo)));
	}

	private Long idRegistrado(String json) throws Exception {
		String cuerpo = registrar(json).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return Long.valueOf(JsonPath.read(cuerpo, "$.id").toString());
	}

	private ResultActions registrar(String json) throws Exception {
		return mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String referido(String cantidad, String precio) {
		return referido(btc, "2026-10-13", "\"Programa de referidos Hapi\"", cantidad, precio);
	}

	private String referidoConComentario(String comentarioJson) {
		return """
				{"activoId": %d, "fechaReferido": "2026-10-13", "origen": "Hapi",
				 "cantidad": 1, "precioAlRecibir": 100, "comentario": %s}
				""".formatted(btc, comentarioJson);
	}

	private static String referido(Long activoId, String fecha, String origenJson, String cantidad, String precio) {
		return """
				{"activoId": %d, "fechaReferido": "%s", "origen": %s, "cantidad": %s, "precioAlRecibir": %s}
				""".formatted(activoId, fecha, origenJson, cantidad, precio);
	}

}
