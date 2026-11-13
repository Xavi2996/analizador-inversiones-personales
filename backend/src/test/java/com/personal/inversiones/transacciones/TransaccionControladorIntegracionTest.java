package com.personal.inversiones.transacciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
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

import com.personal.inversiones.activos.modelo.Activo;
import com.personal.inversiones.activos.modelo.Moneda;
import com.personal.inversiones.activos.modelo.TipoActivo;
import com.personal.inversiones.activos.repositorio.ActivoRepositorio;
import com.personal.inversiones.compartido.PruebaIntegracion;
import com.personal.inversiones.transacciones.modelo.Transaccion;
import com.personal.inversiones.transacciones.repositorio.TransaccionRepositorio;

class TransaccionControladorIntegracionTest extends PruebaIntegracion {

	private static final String RUTA = "/api/v1/transacciones";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ActivoRepositorio activoRepositorio;

	@Autowired
	private TransaccionRepositorio transaccionRepositorio;

	@Autowired
	private Clock clock;

	private Long ivv;

	@BeforeEach
	void crearActivo() {
		ivv = activoRepositorio.save(new Activo("IVV", "iShares Core S&P 500 ETF", TipoActivo.ETF, Moneda.USD))
				.getId();
	}

	// --- Cálculos ---

	@Test
	void registrarCompraCalculaMontoBrutoYEfectivoNeto() throws Exception {
		registrar("""
				{"activoId": %d, "tipoTransaccion": "COMPRA", "fechaTransaccion": "2026-10-22",
				 "cantidad": 0.14553, "precioUnitario": 769.57, "comision": 0.15, "comentario": "  Compra IVV  "}
				""".formatted(ivv))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.activoId").value(ivv))
				.andExpect(jsonPath("$.simbolo").value("IVV"))
				.andExpect(jsonPath("$.nombreActivo").value("iShares Core S&P 500 ETF"))
				.andExpect(jsonPath("$.tipoTransaccion").value("COMPRA"))
				.andExpect(jsonPath("$.fechaTransaccion").value("2026-10-22"))
				.andExpect(jsonPath("$.cantidad").value(0.14553))
				.andExpect(jsonPath("$.precioUnitario").value(769.57))
				.andExpect(jsonPath("$.montoBruto").value(112.0))
				.andExpect(jsonPath("$.comision").value(0.15))
				.andExpect(jsonPath("$.efectivoNeto").value(-112.15))
				.andExpect(jsonPath("$.comentario").value("Compra IVV"))
				.andExpect(jsonPath("$.fechaCreacion").doesNotExist())
				.andExpect(jsonPath("$.fechaActualizacion").doesNotExist());

		Transaccion guardada = transaccionRepositorio.findAll().getFirst();
		assertThat(guardada.getMontoBruto()).isEqualByComparingTo("112.00");
		assertThat(guardada.getComentario()).isEqualTo("Compra IVV");
	}

	@Test
	void registrarDevuelveLasEscalasDelContrato() throws Exception {
		registrar(compra("0.14553", "769.57", "0.15"))
				.andExpect(status().isCreated())
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("\"cantidad\":0.14553000", "\"precioUnitario\":769.57",
								"\"montoBruto\":112.00", "\"comision\":0.15", "\"efectivoNeto\":-112.15"));
	}

	@Test
	void registrarConservaLosDecimalesDelPrecio() throws Exception {
		registrar(compra("1", "81990.123456", "0"))
				.andExpect(status().isCreated())
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("\"precioUnitario\":81990.123456", "\"montoBruto\":81990.12"));
	}

	@Test
	void registrarDevuelvePrecioEnteroConDosDecimales() throws Exception {
		registrar(compra("0.00012254", "81606", "0.10"))
				.andExpect(status().isCreated())
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("\"precioUnitario\":81606.00", "\"montoBruto\":10.00", "\"efectivoNeto\":-10.10"));
	}

	@Test
	void montoBrutoRedondeaLaMitadHaciaArriba() throws Exception {
		// 0.5 × 0.01 = 0.005 → 0.01. Debe coincidir con ck_transaccion_monto_bruto (ROUND de PostgreSQL).
		registrar(compra("0.5", "0.01", "0"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.montoBruto").value(0.01));
	}

	@Test
	void registrarIgnoraMontoBrutoYEfectivoNetoEnviados() throws Exception {
		registrar("""
				{"activoId": %d, "tipoTransaccion": "COMPRA", "fechaTransaccion": "2026-10-22",
				 "cantidad": 0.14553, "precioUnitario": 769.57, "comision": 0.15,
				 "montoBruto": 999.99, "efectivoNeto": 5.00}
				""".formatted(ivv))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.montoBruto").value(112.0))
				.andExpect(jsonPath("$.efectivoNeto").value(-112.15));

		assertThat(transaccionRepositorio.findAll().getFirst().getMontoBruto()).isEqualByComparingTo("112.00");
	}

	// --- Validaciones de campos ---

	@Test
	void registrarConCantidadOPrecioCeroONegativoDevuelve400() throws Exception {
		for (String valor : new String[] { "0", "-1" }) {
			registrar(compra(valor, "100", "0"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
					.andExpect(jsonPath("$.erroresCampos[0].campo").value("cantidad"))
					.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Debe ser mayor que cero."));
			registrar(compra("1", valor, "0"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.erroresCampos[0].campo").value("precioUnitario"))
					.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Debe ser mayor que cero."));
		}
		assertThat(transaccionRepositorio.count()).isZero();
	}

	@Test
	void registrarConComisionNegativaDevuelve400() throws Exception {
		registrar(compra("1", "100", "-0.01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("comision"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("No puede ser negativo."));
	}

	@Test
	void registrarConComisionCeroEsValido() throws Exception {
		registrar(compra("1", "100", "0"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.comision").value(0.0))
				.andExpect(jsonPath("$.efectivoNeto").value(-100.0));
	}

	@Test
	void registrarConExcesoDeDecimalesDevuelve400() throws Exception {
		registrar(compra("0.123456789", "100.1234567", "0.151"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos.length()").value(3))
				.andExpect(jsonPath("$.erroresCampos[*].campo")
						.value(containsInAnyOrder("cantidad", "precioUnitario", "comision")));
		assertThat(transaccionRepositorio.count()).isZero();
	}

	@Test
	void registrarConElMaximoDeDecimalesEsValido() throws Exception {
		registrar(compra("0.12345678", "100.123456", "0.15")).andExpect(status().isCreated());
	}

	@Test
	void registrarConExcesoDeDigitosEnterosDevuelve400() throws Exception {
		registrar(compra("12345678901234567", "1", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("cantidad"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje")
						.value("Admite como máximo 8 decimales y 16 dígitos enteros."));
	}

	@Test
	void registrarConFechaDeHoyEsValido() throws Exception {
		registrar(transaccion(ivv, "COMPRA", LocalDate.now(clock).toString(), "1", "100", "0"))
				.andExpect(status().isCreated());
	}

	@Test
	void registrarConFechaFuturaDevuelve400() throws Exception {
		registrar(transaccion(ivv, "COMPRA", LocalDate.now(clock).plusDays(1).toString(), "1", "100", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("fechaTransaccion"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("No puede ser una fecha futura."));
		assertThat(transaccionRepositorio.count()).isZero();
	}

	@Test
	void registrarSinCamposObligatoriosDevuelve400() throws Exception {
		registrar("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos.length()").value(6))
				.andExpect(jsonPath("$.erroresCampos[*].campo").value(containsInAnyOrder("activoId",
						"tipoTransaccion", "fechaTransaccion", "cantidad", "precioUnitario", "comision")))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Es obligatorio."));
	}

	@Test
	void registrarConTipoInvalidoDevuelve400() throws Exception {
		registrar(transaccion(ivv, "CANJE", "2026-10-22", "1", "100", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("tipoTransaccion"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje")
						.value("Valor no permitido. Valores válidos: COMPRA, VENTA."));
	}

	@Test
	void registrarConActivoInexistenteDevuelve404() throws Exception {
		registrar(transaccion(999L, "COMPRA", "2026-10-22", "1", "100", "0"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.codigo").value("ACTIVO_NO_ENCONTRADO"));
		assertThat(transaccionRepositorio.count()).isZero();
	}

	@Test
	void registrarConComentarioDe501CaracteresDevuelve400() throws Exception {
		registrar(compraConComentario("\"" + "C".repeat(501) + "\""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("comentario"))
				.andExpect(jsonPath("$.erroresCampos[0].mensaje").value("Admite como máximo 500 caracteres."));
	}

	@Test
	void registrarConComentarioDe500CaracteresEsValido() throws Exception {
		registrar(compraConComentario("\"" + "C".repeat(500) + "\"")).andExpect(status().isCreated());
	}

	@Test
	void registrarGuardaComentarioVacioComoNull() throws Exception {
		registrar(compraConComentario("\"   \""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.comentario").doesNotExist());
		assertThat(transaccionRepositorio.findAll().getFirst().getComentario()).isNull();
	}

	// --- Bloqueo del símbolo del activo (01-modelo-funcional §2.4) ---

	@Test
	void activoSinTransaccionesPermiteCambiarElSimbolo() throws Exception {
		mockMvc.perform(get("/api/v1/activos/" + ivv))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.simboloEditable").value(true));
		actualizarActivo("IVVX", "ETF")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.simbolo").value("IVVX"));
	}

	@Test
	void activoConTransaccionesBloqueaElSimbolo() throws Exception {
		registrar(compra("1", "100", "0")).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/activos/" + ivv))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.simboloEditable").value(false));
		actualizarActivo("IVVX", "ETF")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.codigo").value("SIMBOLO_BLOQUEADO"))
				.andExpect(jsonPath("$.erroresCampos[0].campo").value("simbolo"));

		assertThat(activoRepositorio.findById(ivv).orElseThrow().getSimbolo()).isEqualTo("IVV");
	}

	@Test
	void activoConTransaccionesPermiteCambiarElTipo() throws Exception {
		registrar(compra("1", "100", "0")).andExpect(status().isCreated());

		actualizarActivo("IVV", "ACCION")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.simbolo").value("IVV"))
				.andExpect(jsonPath("$.tipoActivo").value("ACCION"));
	}

	// --- Utilidades ---

	private ResultActions actualizarActivo(String simbolo, String tipoActivo) throws Exception {
		return mockMvc.perform(put("/api/v1/activos/" + ivv).contentType(MediaType.APPLICATION_JSON).content("""
				{"simbolo": "%s", "nombre": "iShares Core S&P 500 ETF", "tipoActivo": "%s", "moneda": "USD"}
				""".formatted(simbolo, tipoActivo)));
	}

	private ResultActions registrar(String json) throws Exception {
		return mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String compra(String cantidad, String precio, String comision) {
		return transaccion(ivv, "COMPRA", "2026-10-22", cantidad, precio, comision);
	}

	private String compraConComentario(String comentarioJson) {
		return """
				{"activoId": %d, "tipoTransaccion": "COMPRA", "fechaTransaccion": "2026-10-22",
				 "cantidad": 1, "precioUnitario": 100, "comision": 0, "comentario": %s}
				""".formatted(ivv, comentarioJson);
	}

	private static String transaccion(Long activoId, String tipo, String fecha, String cantidad, String precio,
			String comision) {
		return """
				{"activoId": %d, "tipoTransaccion": "%s", "fechaTransaccion": "%s",
				 "cantidad": %s, "precioUnitario": %s, "comision": %s}
				""".formatted(activoId, tipo, fecha, cantidad, precio, comision);
	}

}
