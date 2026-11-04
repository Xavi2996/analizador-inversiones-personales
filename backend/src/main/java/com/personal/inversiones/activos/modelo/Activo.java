package com.personal.inversiones.activos.modelo;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Instrumento financiero que puede formar parte del portafolio (01-modelo-funcional §2).
 * El símbolo llega ya normalizado desde el servicio.
 */
@Entity
@Table(name = "activo")
public class Activo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 20)
	private String simbolo;

	@Column(nullable = false, length = 150)
	private String nombre;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_activo", nullable = false, length = 30)
	private TipoActivo tipoActivo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Moneda moneda;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private OffsetDateTime fechaCreacion;

	@Column(name = "fecha_actualizacion", nullable = false)
	private OffsetDateTime fechaActualizacion;

	protected Activo() {
	}

	public Activo(String simbolo, String nombre, TipoActivo tipoActivo, Moneda moneda) {
		this.simbolo = simbolo;
		this.nombre = nombre;
		this.tipoActivo = tipoActivo;
		this.moneda = moneda;
	}

	public void actualizar(String simbolo, String nombre, TipoActivo tipoActivo, Moneda moneda) {
		this.simbolo = simbolo;
		this.nombre = nombre;
		this.tipoActivo = tipoActivo;
		this.moneda = moneda;
	}

	@PrePersist
	void alCrear() {
		OffsetDateTime ahora = OffsetDateTime.now();
		fechaCreacion = ahora;
		fechaActualizacion = ahora;
	}

	@PreUpdate
	void alActualizar() {
		fechaActualizacion = OffsetDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getSimbolo() {
		return simbolo;
	}

	public String getNombre() {
		return nombre;
	}

	public TipoActivo getTipoActivo() {
		return tipoActivo;
	}

	public Moneda getMoneda() {
		return moneda;
	}

}
