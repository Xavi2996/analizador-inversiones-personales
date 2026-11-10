package com.personal.inversiones.aportes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.personal.inversiones.compartido.PruebaIntegracion;

/**
 * Comparación con la hoja Aportes de referencia/registro_inversiones_v4_1.xlsx (spec 002, paso 8).
 *
 * <pre>
 * Fecha      | Aporte real | Neto broker | Costo fondeo (=B-C) | Método / ruta               | Nota
 * 2026-10-13 | 125         | 120.22      | 4.78                | Binance USDT → ERC20 → Hapi | Primer aporte
 * 2026-10-19 | 250         | 243.33      | 6.67                | Binance USDT → ERC20 → Hapi | Segundo aporte
 * 2026-10-21 | 200         | 195.58      | 4.42                | Binance USDT → ERC20 → Hapi | Tercer aporte
 *
 * Resumen!B4 = SUM(Aportes!B) = 575
 * Resumen!B5 = SUM(Aportes!C) = 559.13
 * Resumen!B6 = SUM(Aportes!D) = 15.87
 * </pre>
 *
 * El Excel calcula el costo con coma flotante (4.7800000000000011, 15.869999999999976); la API usa
 * BigDecimal y devuelve los valores exactos a 2 decimales. El Excel no calcula porcentajes: los
 * esperados salen de la regla de la spec (parte × 100 / total, 2 decimales, HALF_UP):
 * 15.87 × 100 / 575 = 2.76, 559.13 × 100 / 575 = 97.24, 4.78 / 125 = 3.82, 6.67 / 250 = 2.67 y 4.42 / 200 = 2.21.
 */
class ComparacionExcelAportesTest extends PruebaIntegracion {

	private static final String METODO = "Binance USDT → ERC20 → Hapi";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void losAportesYTotalesDelExcelCoincidenConLaApi() throws Exception {
		registrar("2026-10-13", "125.00", "120.22", "Primer aporte")
				.andExpect(jsonPath("$.costoFondeo").value(4.78));
		registrar("2026-10-19", "250.00", "243.33", "Segundo aporte")
				.andExpect(jsonPath("$.costoFondeo").value(6.67));
		registrar("2026-10-21", "200.00", "195.58", "Tercer aporte")
				.andExpect(jsonPath("$.costoFondeo").value(4.42));

		mockMvc.perform(get("/api/v1/aportes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aportes.length()").value(3))
				.andExpect(jsonPath("$.aportes[0].fechaAporte").value("2026-10-21"))
				.andExpect(jsonPath("$.aportes[0].montoAportado").value(200.0))
				.andExpect(jsonPath("$.aportes[0].montoNetoBroker").value(195.58))
				.andExpect(jsonPath("$.aportes[0].costoFondeo").value(4.42))
				.andExpect(jsonPath("$.aportes[0].porcentajeCostoFondeo").value(2.21))
				.andExpect(jsonPath("$.aportes[0].metodoFondeo").value(METODO))
				.andExpect(jsonPath("$.aportes[0].comentario").value("Tercer aporte"))
				.andExpect(jsonPath("$.aportes[1].fechaAporte").value("2026-10-19"))
				.andExpect(jsonPath("$.aportes[1].montoAportado").value(250.0))
				.andExpect(jsonPath("$.aportes[1].montoNetoBroker").value(243.33))
				.andExpect(jsonPath("$.aportes[1].costoFondeo").value(6.67))
				.andExpect(jsonPath("$.aportes[1].porcentajeCostoFondeo").value(2.67))
				.andExpect(jsonPath("$.aportes[2].fechaAporte").value("2026-10-13"))
				.andExpect(jsonPath("$.aportes[2].montoAportado").value(125.0))
				.andExpect(jsonPath("$.aportes[2].montoNetoBroker").value(120.22))
				.andExpect(jsonPath("$.aportes[2].costoFondeo").value(4.78))
				.andExpect(jsonPath("$.aportes[2].porcentajeCostoFondeo").value(3.82))
				// Resumen!B4:B6
				.andExpect(jsonPath("$.totalAportado").value(575.0))
				.andExpect(jsonPath("$.totalNetoBroker").value(559.13))
				.andExpect(jsonPath("$.totalCostoFondeo").value(15.87))
				.andExpect(jsonPath("$.porcentajeCostoFondeo").value(2.76))
				.andExpect(jsonPath("$.porcentajeNetoBroker").value(97.24));
	}

	private ResultActions registrar(String fecha, String aportado, String neto, String comentario) throws Exception {
		return mockMvc.perform(post("/api/v1/aportes").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"fechaAporte":"%s","montoAportado":%s,"montoNetoBroker":%s,
						 "metodoFondeo":"%s","comentario":"%s"}
						""".formatted(fecha, aportado, neto, METODO, comentario)))
				.andExpect(status().isCreated());
	}

}
