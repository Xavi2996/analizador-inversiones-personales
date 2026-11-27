package com.personal.inversiones.referidos.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.personal.inversiones.activos.modelo.Activo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Activo recibido mediante un programa de referidos (01-modelo-funcional §5).
 * Los valores llegan validados y normalizados desde el servicio, que también calcula el valor recibido.
 * Cantidad, precio al recibir y valor recibido son históricos. En el MVP un referido no se edita ni se elimina.
 */
@Entity
@Table(name = "referido")
public class Referido {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "activo_id", nullable = false, updatable = false)
	private Activo activo;

	@Column(name = "fecha_referido", nullable = false, updatable = false)
	private LocalDate fechaReferido;

	@Column(nullable = false, updatable = false, length = 100)
	private String origen;

	@Column(nullable = false, updatable = false, precision = 28, scale = 12)
	private BigDecimal cantidad;

	@Column(name = "precio_al_recibir", nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal precioAlRecibir;

	@Column(name = "valor_recibido", nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal valorRecibido;

	@Column(columnDefinition = "TEXT", updatable = false)
	private String comentario;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private OffsetDateTime fechaCreacion;

	@Column(name = "fecha_actualizacion", nullable = false)
	private OffsetDateTime fechaActualizacion;

	protected Referido() {
	}

	public Referido(Activo activo, LocalDate fechaReferido, String origen, BigDecimal cantidad,
			BigDecimal precioAlRecibir, BigDecimal valorRecibido, String comentario) {
		this.activo = activo;
		this.fechaReferido = fechaReferido;
		this.origen = origen;
		this.cantidad = cantidad;
		this.precioAlRecibir = precioAlRecibir;
		this.valorRecibido = valorRecibido;
		this.comentario = comentario;
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

	public LocalDate getFechaReferido() {
		return fechaReferido;
	}

	public String getOrigen() {
		return origen;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public BigDecimal getPrecioAlRecibir() {
		return precioAlRecibir;
	}

	public BigDecimal getValorRecibido() {
		return valorRecibido;
	}

	public String getComentario() {
		return comentario;
	}

}
