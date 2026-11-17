package com.personal.inversiones.transacciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.personal.inversiones.compartido.PruebaIntegracion;

/**
 * Comparación con la hoja Transacciones de referencia/registro_inversiones_v4_1.xlsx (spec 003, paso 11).
 *
 * <pre>
 * Fecha      | Activo | Mov.   | Cantidad   | Precio   | Monto (G) | Comisión | Efectivo neto (I) | Nota
 * 2026-10-13 | IVV    | COMPRA | 0.15368    | 781.28   | 120.07    | 0.15     | -120.22           | Primera compra IVV
 * 2026-10-19 | IVV    | COMPRA | 0.24387    | 770.89   | 188       | 0.15     | -188.15           | Segunda compra IVV
 * 2026-10-19 | VXUS   | COMPRA | 0.43171    | 83.38    | 36        | 0.15     | -36.15            | Primera compra VXUS
 * 2026-10-19 | BTC    | COMPRA | 0.00020734 | 81990.93 | 17        | 0.17     | -17.17            | Compra bit Coin
 * 2026-10-22 | IVV    | COMPRA | 0.14553    | 769.57   | 112       | 0.15     | -112.15           | Compra IVV
 * 2026-10-22 | VXUS   | COMPRA | 0.24102    | 82.97    | 20        | 0.15     | -20.15            | Compra VXUS
 * 2026-10-22 | BTC    | COMPRA | 0.00012254 | 81606    | 10        | 0.10     | -10.10            | Compra BTC
 *
 * Resumen!B7  = monto utilizado en compras = 503.07
 * Resumen!B8  = comisiones                 = 1.02
 * Resumen!B11 = efectivo disponible        = 55.04 (559.13 neto de aportes − 504.09; sin dividendos)
 * Posiciones!C5:C7 = IVV 0.54308, VXUS 0.67273, BTC 0.00032988 (sin referidos)
 * </pre>
 *
 * El Excel ingresa el monto (G) como importe del broker; la API lo calcula como
 * cantidad × precio redondeado a 2 decimales HALF_UP y reproduce los 7 valores. El Excel guarda cantidades
 * y precios con coma flotante (0.15368000000000001, 81990.929999999993); la API los conserva exactos.
 * El efectivo disponible (FASE 9) aún no existe: aquí se comprueba que neto de aportes + Σ efectivoNeto
 * da el valor del Excel con los datos que ya expone la API.
 */
class ComparacionExcelTransaccionesTest extends PruebaIntegracion {

	private static final String RUTA = "/api/v1/transacciones";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void lasComprasYTotalesDelExcelCoincidenConLaApi() throws Exception {
		long ivv = registrarActivo("IVV", "iShares Core S&P 500 ETF", "ETF");
		long vxus = registrarActivo("VXUS", "Vanguard Total International Stock ETF", "ETF");
		long btc = registrarActivo("BTC", "Bitcoin", "CRIPTOMONEDA");

		registrarAporte("2026-10-13", "125.00", "120.22");
		registrarAporte("2026-10-19", "250.00", "243.33");
		registrarAporte("2026-10-21", "200.00", "195.58");

		comprar(ivv, "2026-10-13", "0.15368", "781.28", "0.15", "Primera compra IVV", 120.07, -120.22);
		comprar(ivv, "2026-10-19", "0.24387", "770.89", "0.15", "Segunda compra IVV", 188.00, -188.15);
		comprar(vxus, "2026-10-19", "0.43171", "83.38", "0.15", "Primera compra VXUS", 36.00, -36.15);
		comprar(btc, "2026-10-19", "0.00020734", "81990.93", "0.17", "Compra bit Coin", 17.00, -17.17);
		comprar(ivv, "2026-10-22", "0.14553", "769.57", "0.15", "Compra IVV", 112.00, -112.15);
		comprar(vxus, "2026-10-22", "0.24102", "82.97", "0.15", "Compra VXUS", 20.00, -20.15);
		comprar(btc, "2026-10-22", "0.00012254", "81606", "0.10", "Compra BTC", 10.00, -10.10);

		String lista = mockMvc.perform(get(RUTA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.transacciones.length()").value(7))
				// Resumen!B7:B8
				.andExpect(jsonPath("$.totalComprado").value(503.07))
				.andExpect(jsonPath("$.totalVendido").value(0.0))
				.andExpect(jsonPath("$.totalComisiones").value(1.02))
				.andExpect(jsonPath("$.conteoCompras").value(7))
				.andExpect(jsonPath("$.conteoVentas").value(0))
				.andReturn().getResponse().getContentAsString();

		// Posiciones!C5:C7: la cantidad disponible a hoy coincide con la cantidad actual (sin referidos).
		disponible(ivv, "0.54308000");
		disponible(vxus, "0.67273000");
		disponible(btc, "0.00032988");

		// Resumen!B11: neto de aportes (Resumen!B5) + Σ efectivoNeto de transacciones.
		String aportes = mockMvc.perform(get("/api/v1/aportes")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		BigDecimal netoAportes = new BigDecimal(JsonPath.read(aportes, "$.totalNetoBroker").toString());
		List<Object> efectivos = JsonPath.read(lista, "$.transacciones[*].efectivoNeto");
		BigDecimal efectivoTransacciones = efectivos.stream()
				.map(valor -> new BigDecimal(valor.toString()))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		assertThat(netoAportes).isEqualByComparingTo("559.13");
		assertThat(efectivoTransacciones).isEqualByComparingTo("-504.09");
		assertThat(netoAportes.add(efectivoTransacciones)).isEqualByComparingTo("55.04");
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

	private void registrarAporte(String fecha, String aportado, String neto) throws Exception {
		mockMvc.perform(post("/api/v1/aportes").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fechaAporte":"%s","montoAportado":%s,"montoNetoBroker":%s}
						""".formatted(fecha, aportado, neto)))
				.andExpect(status().isCreated());
	}

	private ResultActions comprar(long activoId, String fecha, String cantidad, String precio, String comision,
			String nota, double montoBruto, double efectivoNeto) throws Exception {
		return mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"activoId":%d,"tipoTransaccion":"COMPRA","fechaTransaccion":"%s","cantidad":%s,
						 "precioUnitario":%s,"comision":%s,"comentario":"%s"}
						""".formatted(activoId, fecha, cantidad, precio, comision, nota)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.montoBruto").value(montoBruto))
				.andExpect(jsonPath("$.efectivoNeto").value(efectivoNeto));
	}

	private void disponible(long activoId, String esperado) throws Exception {
		mockMvc.perform(get(RUTA + "/disponible").param("activoId", String.valueOf(activoId)))
				.andExpect(status().isOk())
				.andExpect(result -> assertThat(result.getResponse().getContentAsString())
						.contains("\"cantidadDisponible\":" + esperado));
	}

}
