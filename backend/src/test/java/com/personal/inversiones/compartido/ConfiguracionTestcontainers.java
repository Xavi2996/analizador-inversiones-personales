package com.personal.inversiones.compartido;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL 17 real para las pruebas de integración. Spring gestiona su ciclo de vida,
 * así que el contenedor se comparte entre las clases que reutilizan el mismo contexto.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ConfiguracionTestcontainers {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
	}

}
