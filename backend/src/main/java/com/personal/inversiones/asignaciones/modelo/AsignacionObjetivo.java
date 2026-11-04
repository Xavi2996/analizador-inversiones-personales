package com.personal.inversiones.asignaciones.modelo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.personal.inversiones.activos.modelo.Activo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Porcentaje deseado del portafolio para un activo (01-modelo-funcional §8).
 * Cada activo tiene como máximo una asignación; sin fila, el activo no tiene objetivo.
 */
@Entity
@Table(name = "asignacion_objetivo")
public class AsignacionObjetivo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "activo_id", nullable = false, updatable = false)
	private Activo activo;

	@Column(name = "porcentaje_objetivo", nullable = false, precision = 8, scale = 4)
	private BigDecimal porcentajeObjetivo;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private OffsetDateTime fechaCreacion;

	@Column(name = "fecha_actualizacion", nullable = false)
	private OffsetDateTime fechaActualizacion;

	protected AsignacionObjetivo() {
	}

	public AsignacionObjetivo(Activo activo, BigDecimal porcentajeObjetivo) {
		this.activo = activo;
		this.porcentajeObjetivo = porcentajeObjetivo;
	}

	public void actualizarPorcentaje(BigDecimal porcentajeObjetivo) {
		this.porcentajeObjetivo = porcentajeObjetivo;
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

	public Activo getActivo() {
		return activo;
	}

	public BigDecimal getPorcentajeObjetivo() {
		return porcentajeObjetivo;
	}

}
