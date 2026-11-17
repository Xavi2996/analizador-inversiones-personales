package com.personal.inversiones.transacciones.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Los totales se calculan sobre la lista filtrada por activo y tipo. Los conteos solo aplican el filtro
 * de activo, para las pestañas Todas / Compras / Ventas (SPEC 003 §3.6).
 */
public record ListaTransaccionesRespuesta(
		List<TransaccionRespuesta> transacciones,
		BigDecimal totalComprado,
		BigDecimal totalVendido,
		BigDecimal totalComisiones,
		long conteoCompras,
		long conteoVentas) {
}
