package com.personal.inversiones.referidos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;
import com.personal.inversiones.compartido.PruebaIntegracion;

/**
 * Comparación con la hoja Referidos de referencia/registro_inversiones_v4_1.xlsx (spec 004, paso 10).
 *
 * <pre>
 * Fecha      | Origen   | Activo | Cantidad   | Precio al recibir | Valor recibido (F5) | Nota
 * 2026-10-13 | Referido | BTC    | 0.00001196 | 83580.27          | 1                   | Premio Hapi por referido
 *
 * Posiciones!E7 = cantidad referida BTC = 0.00001196
 * Posiciones!F5:F7 = cantidad actual IVV 0.54308, VXUS 0.67273, BTC 0.00034184 (0.00032988 + 0.00001196)
 * Resumen!B7 = monto utilizado en compras = 503.07 (el referido no es compra)
 * </pre>
 *
 * El Excel ingresa el valor recibido (F5) a mano; la API lo calcula como cantidad × precio al recibir
 * redondeado a 2 decimales HALF_UP: 0.00001196 × 83580.27 = 0.9996200... → 1.00. Las columnas G:J del Excel
 * (precio y valor actual, variación, rendimiento) llegan con Precios; la columna K (costo desde bolsillo,
 * siempre 0) no se guarda (spec 004 §6).
 */
class ComparacionExcelReferidosTest extends PruebaIntegracion {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void elReferidoDelExcelYSusEfectosCoincidenConLaApi() throws Exception {
		long ivv = registrarActivo("IVV", "iShares Core S&P 500 ETF", "ETF");
		long vxus = registrarActivo("VXUS", "Vanguard Total International Stock ETF", "ETF");
		long btc = registrarActivo("BTC", "Bitcoin", "CRIPTOMONEDA");

		// Hoja Transacciones (las 7 compras de SPEC 003).
		comprar(ivv, "2026-10-13", "0.15368", "781.28", "0.15");
		comprar(ivv, "2026-10-19", "0.24387", "770.89", "0.15");
		comprar(vxus, "2026-10-19", "0.43171", "83.38", "0.15");
		comprar(btc, "2026-10-19", "0.00020734", "81990.93", "0.17");
		comprar(ivv, "2026-10-22", "0.14553", "769.57", "0.15");
		comprar(vxus, "2026-10-22", "0.24102", "82.97", "0.15");
		comprar(btc, "2026-10-22", "0.00012254", "81606", "0.10");

		// Referidos!A5:F5 y L5.
		mockMvc.perform(post("/api/v1/referidos").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"activoId":%d,"fechaReferido":"2026-10-13","origen":"Programa de referidos Hapi",
						 "cantidad":0.00001196,"precioAlRecibir":83580.27,
						 "comentario":"Premio Hapi por referido: $1.00 en BTC (13 oct 2026)"}
						""".formatted(btc)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.simbolo").value("BTC"))
				// Referidos!F5
				.andExpect(jsonPath("$.valorRecibido").value(1.00))
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("\"cantidad\":0.00001196")
						.contains("\"valorRecibido\":1.00"));

		mockMvc.perform(get("/api/v1/referidos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.referidos.length()").value(1))
				.andExpect(jsonPath("$.totalValorRecibido").value(1.00))
				.andExpect(jsonPath("$.conteo").value(1));

		// Posiciones!F5:F7: el referido suma a BTC; IVV y VXUS no cambian.
		disponible(btc, "0.00034184");
		disponible(ivv, "0.54308000");
		disponible(vxus, "0.67273000");

		// Resumen!B7: el referido no es compra.
		mockMvc.perform(get("/api/v1/transacciones"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalComprado").value(503.07))
				.andExpect(jsonPath("$.totalComisiones").value(1.02))
				.andExpect(jsonPath("$.conteoCompras").value(7));
	}

	private long registrarActivo(String simbolo, String nombre, String tipo) throws Exception {
		String respuesta = mockMvc.perform(post("/api/v1/activos").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"simbolo":"%s","nombre":"%s","tipoActivo":"%s","moneda":"USD"}
						""".formatted(simbolo, nombre, tipo)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
	}

	private void comprar(long activoId, String fecha, String cantidad, String precio, String comision)
			throws Exception {
		mockMvc.perform(post("/api/v1/transacciones").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"activoId":%d,"tipoTransaccion":"COMPRA","fechaTransaccion":"%s","cantidad":%s,
						 "precioUnitario":%s,"comision":%s}
						""".formatted(activoId, fecha, cantidad, precio, comision)))
				.andExpect(status().isCreated());
	}

	private void disponible(long activoId, String esperado) throws Exception {
		mockMvc.perform(get("/api/v1/transacciones/disponible").param("activoId", String.valueOf(activoId)))
				.andExpect(status().isOk())
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("\"cantidadDisponible\":" + esperado));
	}

}
