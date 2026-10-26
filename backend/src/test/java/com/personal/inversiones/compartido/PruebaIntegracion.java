package com.personal.inversiones.compartido;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

/**
 * Base de las pruebas de integración: contexto completo, Flyway y PostgreSQL 17 con Testcontainers.
 * Requiere Docker en ejecución.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ConfiguracionTestcontainers.class)
public abstract class PruebaIntegracion {
}
