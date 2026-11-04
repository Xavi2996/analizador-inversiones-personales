package com.personal.inversiones.compartido.error;

/**
 * Se traduce a 404 con el código funcional indicado.
 */
public class RecursoNoEncontradoExcepcion extends RuntimeException {

	private final String codigo;

	public RecursoNoEncontradoExcepcion(String codigo, String mensaje) {
		super(mensaje);
		this.codigo = codigo;
	}

	public String getCodigo() {
		return codigo;
	}

}
