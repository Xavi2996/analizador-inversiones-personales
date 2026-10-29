package com.personal.inversiones.asignaciones.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.personal.inversiones.asignaciones.modelo.AsignacionObjetivo;

public interface AsignacionObjetivoRepositorio extends JpaRepository<AsignacionObjetivo, Long> {

	Optional<AsignacionObjetivo> findByActivoId(Long activoId);

	@Query("select a from AsignacionObjetivo a join fetch a.activo order by a.activo.simbolo")
	List<AsignacionObjetivo> findAllConActivoOrdenadasPorSimbolo();

}
