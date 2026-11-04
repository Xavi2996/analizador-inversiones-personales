package com.personal.inversiones.compartido;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base de las pruebas de integración: contexto completo, Flyway y PostgreSQL 17 con Testcontainers.
 * Requiere Docker en ejecución.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ConfiguracionTestcontainers.class)
public abstract class PruebaIntegracion {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	/**
	 * Cada prueba parte de una base vacía. Se vacían todas las tablas excepto el historial de Flyway,
	 * así las tablas que agreguen futuras migraciones quedan cubiertas sin tocar esta clase.
	 */
	@BeforeEach
	void vaciarBaseDatos() {
		List<String> tablas = jdbcTemplate.queryForList("""
				SELECT tablename FROM pg_tables
				WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
				""", String.class);
		if (!tablas.isEmpty()) {
			jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", tablas) + " RESTART IDENTITY CASCADE");
		}
	}

}
