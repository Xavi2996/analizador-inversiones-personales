package com.personal.inversiones.compartido.tiempo;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de negocio (03-arquitectura §30.1). "Hoy" es la fecha en la zona configurada en
 * {@code app.zona-horaria-negocio}, no la del servidor. Los servicios usan {@code LocalDate.now(clock)}.
 */
@Configuration
public class ConfiguracionReloj {

	@Bean
	public Clock relojNegocio(@Value("${app.zona-horaria-negocio}") ZoneId zonaHorariaNegocio) {
		return Clock.system(zonaHorariaNegocio);
	}

}
