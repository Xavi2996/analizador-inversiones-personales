package com.personal.inversiones.transacciones.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.personal.inversiones.activos.modelo.Activo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Compra o venta de un activo (01-modelo-funcional §4).
 * Los valores llegan validados y normalizados desde el servicio, que también calcula el monto bruto.
 * El efectivo neto no se guarda: se calcula al responder. En el MVP una transacción no se edita ni se elimina.
 */
@Entity
@Table(name = "transaccion")
public class Transaccion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "activo_id", nullable = false, updatable = false)
	private Activo activo;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_transaccion", nullable = false, length = 20, updatable = false)
	private TipoTransaccion tipoTransaccion;

	@Column(name = "fecha_transaccion", nullable = false, updatable = false)
	private LocalDate fechaTransaccion;

	@Column(nullable = false, updatable = false, precision = 28, scale = 12)
	private BigDecimal cantidad;

	@Column(name = "precio_unitario", nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal precioUnitario;

	@Column(name = "monto_bruto", nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal montoBruto;

	@Column(nullable = false, updatable = false, precision = 19, scale = 6)
	private BigDecimal comision;

	@Column(columnDefinition = "TEXT", updatable = false)
	private String comentario;

	@Column(name = "fecha_creacion", nullable = false, updatable = false)
	private OffsetDateTime fechaCreacion;

	@Column(name = "fecha_actualizacion", nullable = false)
	private OffsetDateTime fechaActualizacion;

	protected Transaccion() {
	}

	public Transaccion(Activo activo, TipoTransaccion tipoTransaccion, LocalDate fechaTransaccion,
			BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal montoBruto, BigDecimal comision,
			String comentario) {
		this.activo = activo;
		this.tipoTransaccion = tipoTransaccion;
		this.fechaTransaccion = fechaTransaccion;
		this.cantidad = cantidad;
		this.precioUnitario = precioUnitario;
		this.montoBruto = montoBruto;
		this.comision = comision;
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

	public TipoTransaccion getTipoTransaccion() {
		return tipoTransaccion;
	}

	public LocalDate getFechaTransaccion() {
		return fechaTransaccion;
	}

	public BigDecimal getCantidad() {
		return cantidad;
	}

	public BigDecimal getPrecioUnitario() {
		return precioUnitario;
	}

	public BigDecimal getMontoBruto() {
		return montoBruto;
	}

	public BigDecimal getComision() {
		return comision;
	}

	public String getComentario() {
		return comentario;
	}

}
