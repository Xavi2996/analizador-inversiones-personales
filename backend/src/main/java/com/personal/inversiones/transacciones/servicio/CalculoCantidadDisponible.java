package com.personal.inversiones.transacciones.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Cantidad disponible para vender de un activo en una fecha (01-modelo-funcional §4.7, SPEC 003 §3.3).
 *
 * <pre>
 * saldo(D)      = Σ movimientos con fecha ≤ D
 * disponible(D) = min( {saldo(D)} ∪ {saldo(F) : F > D y F tiene movimientos} )
 * </pre>
 *
 * Los movimientos de un mismo día cuentan como simultáneos: se agrupan por fecha antes de acumular.
 * Así una venta en D nunca deja el saldo negativo en D ni en ninguna fecha posterior.
 * Lógica pura, sin acceso a datos.
 */
public final class CalculoCantidadDisponible {

	/**
	 * Movimiento de cantidad con signo: positivo si entra al portafolio (compra; en el futuro, referido),
	 * negativo si sale (venta).
	 */
	public record Movimiento(LocalDate fecha, BigDecimal cantidad) {
	}

	private CalculoCantidadDisponible() {
	}

	public static BigDecimal calcular(List<Movimiento> movimientos, LocalDate fecha) {
		Map<LocalDate, BigDecimal> netoPorFecha = new TreeMap<>();
		for (Movimiento movimiento : movimientos) {
			netoPorFecha.merge(movimiento.fecha(), movimiento.cantidad(), BigDecimal::add);
		}

		BigDecimal saldo = BigDecimal.ZERO;
		BigDecimal disponible = null;
		for (Map.Entry<LocalDate, BigDecimal> entrada : netoPorFecha.entrySet()) {
			if (disponible == null && entrada.getKey().isAfter(fecha)) {
				// saldo(D) siempre forma parte del mínimo, aunque D no tenga movimientos.
				disponible = saldo;
			}
			saldo = saldo.add(entrada.getValue());
			if (disponible != null) {
				disponible = disponible.min(saldo);
			}
		}
		// Sin movimientos posteriores a D: disponible(D) = saldo(D).
		return disponible == null ? saldo : disponible;
	}

}
