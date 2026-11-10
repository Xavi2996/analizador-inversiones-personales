package com.personal.inversiones.aportes.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AporteRespuesta(
		Long id,
		LocalDate fechaAporte,
		BigDecimal montoAportado,
		BigDecimal montoNetoBroker,
		BigDecimal costoFondeo,
		BigDecimal porcentajeCostoFondeo,
		String metodoFondeo,
		String comentario) {
}
