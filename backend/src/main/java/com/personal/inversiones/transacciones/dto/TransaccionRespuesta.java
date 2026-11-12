package com.personal.inversiones.transacciones.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.personal.inversiones.transacciones.modelo.TipoTransaccion;

/**
 * {@code efectivoNeto} lleva signo: negativo en compras (sale efectivo) y positivo en ventas (entra efectivo).
 */
public record TransaccionRespuesta(
		Long id,
		Long activoId,
		String simbolo,
		String nombreActivo,
		TipoTransaccion tipoTransaccion,
		LocalDate fechaTransaccion,
		BigDecimal cantidad,
		BigDecimal precioUnitario,
		BigDecimal montoBruto,
		BigDecimal comision,
		BigDecimal efectivoNeto,
		String comentario) {
}
