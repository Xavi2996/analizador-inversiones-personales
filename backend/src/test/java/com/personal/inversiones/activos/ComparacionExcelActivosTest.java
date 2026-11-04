package com.personal.inversiones.activos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.personal.inversiones.activos.repositorio.ActivoRepositorio;
import com.personal.inversiones.compartido.PruebaIntegracion;

/**
 * Comparación con la hoja Activos de referencia/registro_inversiones_v4_1.xlsx (spec 001, paso 12).
 *
 * <pre>
 * Ticker | Nombre                                 | Tipo   | Moneda | Objetivo %
 * IVV    | iShares Core S&P 500 ETF               | ETF    | USD    | 0.85
 * VXUS   | Vanguard Total International Stock ETF | ETF    | USD    | 0.10
 * BTC    | Bitcoin                                | CRIPTO | USD    | 0.05
 * </pre>
 *
 * El Excel guarda el objetivo como fracción (0.85) y la API como porcentaje (85).
 * El tipo CRIPTO del Excel corresponde a CRIPTOMONEDA. Las columnas "Activo?" y "Nota" no se trasladan.
 */
class ComparacionExcelActivosTest extends PruebaIntegracion {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ActivoRepositorio activoRepositorio;

	@Test
	void losActivosYObjetivosDelExcelCoincidenConLaApi() throws Exception {
		registrar("IVV", "iShares Core S&P 500 ETF", "ETF", "85");
		registrar("VXUS", "Vanguard Total International Stock ETF", "ETF", "10");
		registrar("BTC", "Bitcoin", "CRIPTOMONEDA", "5");

		mockMvc.perform(get("/api/v1/activos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3))
				.andExpect(jsonPath("$[0].simbolo").value("BTC"))
				.andExpect(jsonPath("$[0].nombre").value("Bitcoin"))
				.andExpect(jsonPath("$[0].tipoActivo").value("CRIPTOMONEDA"))
				.andExpect(jsonPath("$[0].moneda").value("USD"))
				.andExpect(jsonPath("$[1].simbolo").value("IVV"))
				.andExpect(jsonPath("$[1].nombre").value("iShares Core S&P 500 ETF"))
				.andExpect(jsonPath("$[1].tipoActivo").value("ETF"))
				.andExpect(jsonPath("$[1].moneda").value("USD"))
				.andExpect(jsonPath("$[2].simbolo").value("VXUS"))
				.andExpect(jsonPath("$[2].nombre").value("Vanguard Total International Stock ETF"))
				.andExpect(jsonPath("$[2].tipoActivo").value("ETF"))
				.andExpect(jsonPath("$[2].moneda").value("USD"));

		mockMvc.perform(get("/api/v1/asignaciones"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.asignaciones[0].simbolo").value("BTC"))
				.andExpect(jsonPath("$.asignaciones[0].porcentajeObjetivo").value(5.0))
				.andExpect(jsonPath("$.asignaciones[1].simbolo").value("IVV"))
				.andExpect(jsonPath("$.asignaciones[1].porcentajeObjetivo").value(85.0))
				.andExpect(jsonPath("$.asignaciones[2].simbolo").value("VXUS"))
				.andExpect(jsonPath("$.asignaciones[2].porcentajeObjetivo").value(10.0))
				.andExpect(jsonPath("$.sumaPorcentajes").value(100.0))
				.andExpect(jsonPath("$.sumaCorrecta").value(true));
	}

	private void registrar(String simbolo, String nombre, String tipo, String porcentaje) throws Exception {
		mockMvc.perform(post("/api/v1/activos").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"simbolo":"%s","nombre":"%s","tipoActivo":"%s","moneda":"USD"}
						""".formatted(simbolo, nombre, tipo)))
				.andExpect(status().isCreated());
		Long id = activoRepositorio.findAll().stream()
				.filter(a -> a.getSimbolo().equals(simbolo))
				.findFirst().orElseThrow().getId();
		mockMvc.perform(put("/api/v1/asignaciones/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"porcentajeObjetivo\":" + porcentaje + "}"))
				.andExpect(status().isOk());
	}

}
