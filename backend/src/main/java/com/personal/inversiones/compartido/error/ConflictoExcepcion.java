package com.personal.inversiones.compartido.error;

/**
 * Se traduce a 409 con el código funcional indicado.
 * Si el conflicto se refiere a un campo, se informa también en {@code erroresCampos}.
 */
public class ConflictoExcepcion extends RuntimeException {

	private final String codigo;
	private final String campo;

	public ConflictoExcepcion(String codigo, String mensaje) {
		this(codigo, mensaje, null);
	}

	public ConflictoExcepcion(String codigo, String mensaje, String campo) {
		super(mensaje);
		this.codigo = codigo;
		this.campo = campo;
	}

	public String getCodigo() {
		return codigo;
	}

	public String getCampo() {
		return campo;
	}

}
