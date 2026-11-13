package com.personal.inversiones.transacciones.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.personal.inversiones.transacciones.modelo.Transaccion;

public interface TransaccionRepositorio extends JpaRepository<Transaccion, Long> {

	boolean existsByActivoId(Long activoId);

}
