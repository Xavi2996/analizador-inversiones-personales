package com.personal.inversiones.activos.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.personal.inversiones.activos.modelo.Activo;

import jakarta.persistence.LockModeType;

public interface ActivoRepositorio extends JpaRepository<Activo, Long> {

	List<Activo> findAllByOrderBySimboloAsc();

	boolean existsBySimbolo(String simbolo);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT a FROM Activo a WHERE a.id = :id")
	Optional<Activo> buscarConBloqueoPorId(@Param("id") Long id);

}
