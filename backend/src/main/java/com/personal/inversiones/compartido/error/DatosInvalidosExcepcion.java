package com.personal.inversiones.compartido.error;

import java.util.List;

/**
 * Datos inválidos que solo el servicio puede detectar (reglas entre campos o que dependen del reloj
 * de negocio). Se traduce a 400 {@code DATOS_INVALIDOS} con los campos en {@code erroresCampos},
 * igual que las validaciones de Bean Validation.
 */
public class DatosInvalidosExcepcion extends RuntimeException {

	private final List<ErrorCampo> erroresCampos;

	public DatosInvalidosExcepcion(List<ErrorCampo> erroresCampos) {
		super("La solicitud contiene datos inválidos.");
		this.erroresCampos = List.copyOf(erroresCampos);
	}

	public List<ErrorCampo> getErroresCampos() {
		return erroresCampos;
	}

}
