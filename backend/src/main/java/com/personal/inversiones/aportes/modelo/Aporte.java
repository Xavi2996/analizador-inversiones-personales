package com.personal.inversiones.aportes.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Dinero enviado desde el bolsillo al intermediario (01-modelo-funcional §3).
 * Los valores llegan validados y normalizados desde el servicio, que también calcula el costo de fondeo.
 * En el MVP un aporte no se edita ni se elimina.
 */
@Entity
@Table(name = "aporte")
public class Aporte {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "fecha_aporte", nullable = false, updatable = false)
	private LocalDate fechaAporte;

	@Column(name = "monto_aportado", nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal montoAportado;

	@Column(name = "monto_neto_broker", nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal montoNetoBroker;

	@Column(name = "costo_fondeo", nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal costoFondeo;

	@Column(name = "metodo_fondeo", length = 50, updatable = false)
	private String metodoFondeo;

	@Column(columnDefinition = "TEXT", updatable = false)
	private String comentario;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private OffsetDateTime fechaCreacion;

	@Column(name = "fecha_actualizacion", nullable = false)
	private OffsetDateTime fechaActualizacion;

	protected Aporte() {
	}

	public Aporte(LocalDate fechaAporte, BigDecimal montoAportado, BigDecimal montoNetoBroker,
			BigDecimal costoFondeo, String metodoFondeo, String comentario) {
		this.fechaAporte = fechaAporte;
		this.montoAportado = montoAportado;
		this.montoNetoBroker = montoNetoBroker;
		this.costoFondeo = costoFondeo;
		this.metodoFondeo = metodoFondeo;
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

	public LocalDate getFechaAporte() {
		return fechaAporte;
	}

	public BigDecimal getMontoAportado() {
		return montoAportado;
	}

	public BigDecimal getMontoNetoBroker() {
		return montoNetoBroker;
	}

	public BigDecimal getCostoFondeo() {
		return costoFondeo;
	}

	public String getMetodoFondeo() {
		return metodoFondeo;
	}

	public String getComentario() {
		return comentario;
	}

}
