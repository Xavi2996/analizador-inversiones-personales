package com.personal.inversiones.transacciones.servicio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.personal.inversiones.transacciones.servicio.CalculoCantidadDisponible.Movimiento;

class CalculoCantidadDisponibleTest {

	@Test
	void sinMovimientosElDisponibleEsCero() {
		assertThat(disponible(List.of(), "2026-10-15")).isEqualByComparingTo("0");
	}

	/** Ejemplo de SPEC 003 §3.3: compra 1.0 el 10/10, venta 0.6 el 20/10. */
	@Test
	void ejemploDeLaSpec() {
		List<Movimiento> movimientos = List.of(compra("2026-10-10", "1.0"), venta("2026-10-20", "0.6"));

		// Una venta el 15/10 no puede dejar negativo el saldo del 20/10.
		assertThat(disponible(movimientos, "2026-10-15")).isEqualByComparingTo("0.4");
		assertThat(disponible(movimientos, "2026-10-10")).isEqualByComparingTo("0.4");
		assertThat(disponible(movimientos, "2026-10-20")).isEqualByComparingTo("0.4");
	}

	@Test
	void sinMovimientosPosterioresElDisponibleEsElSaldoDeEsaFecha() {
		List<Movimiento> movimientos = List.of(compra("2026-10-10", "1.0"), venta("2026-10-20", "0.6"));

		assertThat(disponible(movimientos, "2026-10-25")).isEqualByComparingTo("0.4");
	}

	@Test
	void antesDeTodaCompraElDisponibleEsCero() {
		List<Movimiento> movimientos = List.of(compra("2026-10-10", "1.0"), venta("2026-10-20", "0.6"));

		assertThat(disponible(movimientos, "2026-10-05")).isEqualByComparingTo("0");
	}

	@Test
	void unaCompraPosteriorNoAumentaElDisponibleDeUnaFechaAnterior() {
		List<Movimiento> movimientos = List.of(compra("2026-10-10", "1.0"), venta("2026-10-20", "0.6"),
				compra("2026-10-25", "2.0"));

		assertThat(disponible(movimientos, "2026-10-15")).isEqualByComparingTo("0.4");
		assertThat(disponible(movimientos, "2026-10-25")).isEqualByComparingTo("2.4");
	}

	@Test
	void tomaElMenorSaldoEntreTodasLasFechasPosteriores() {
		// Saldos: 01/10 → 2, 05/10 → 1, 08/10 → 0.5, 09/10 → 3.5.
		List<Movimiento> movimientos = List.of(compra("2026-10-01", "2"), venta("2026-10-05", "1"),
				venta("2026-10-08", "0.5"), compra("2026-10-09", "3"));

		assertThat(disponible(movimientos, "2026-10-01")).isEqualByComparingTo("0.5");
		assertThat(disponible(movimientos, "2026-10-06")).isEqualByComparingTo("0.5");
		assertThat(disponible(movimientos, "2026-10-09")).isEqualByComparingTo("3.5");
	}

	@Test
	void unaCompraDelMismoDiaCuentaParaVenderEseDia() {
		List<Movimiento> movimientos = List.of(compra("2026-10-10", "1.0"));

		assertThat(disponible(movimientos, "2026-10-10")).isEqualByComparingTo("1.0");
		assertThat(disponible(movimientos, "2026-10-09")).isEqualByComparingTo("0");
	}

	@Test
	void losMovimientosDelMismoDiaSonSimultaneos() {
		// La venta aparece antes que la compra en la lista: el orden dentro del día no importa.
		List<Movimiento> movimientos = List.of(venta("2026-10-10", "1.0"), compra("2026-10-10", "1.0"),
				compra("2026-10-12", "0.3"));

		assertThat(disponible(movimientos, "2026-10-10")).isEqualByComparingTo("0");
		assertThat(disponible(movimientos, "2026-10-12")).isEqualByComparingTo("0.3");
	}

	@Test
	void elOrdenDeLaListaNoAfectaElResultado() {
		List<Movimiento> movimientos = List.of(venta("2026-10-20", "0.6"), compra("2026-10-10", "1.0"));

		assertThat(disponible(movimientos, "2026-10-15")).isEqualByComparingTo("0.4");
	}

	@Test
	void conservaLaPrecisionDeLasCantidades() {
		List<Movimiento> movimientos = List.of(compra("2026-10-13", "0.00020734"), compra("2026-10-22", "0.00012254"));

		assertThat(disponible(movimientos, "2026-10-22")).isEqualByComparingTo("0.00032988");
	}

	private static BigDecimal disponible(List<Movimiento> movimientos, String fecha) {
		return CalculoCantidadDisponible.calcular(movimientos, LocalDate.parse(fecha));
	}

	private static Movimiento compra(String fecha, String cantidad) {
		return new Movimiento(LocalDate.parse(fecha), new BigDecimal(cantidad));
	}

	private static Movimiento venta(String fecha, String cantidad) {
		return new Movimiento(LocalDate.parse(fecha), new BigDecimal(cantidad).negate());
	}

}
