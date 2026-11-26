package com.personal.inversiones.referidos.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.personal.inversiones.referidos.modelo.Referido;

public interface ReferidoRepositorio extends JpaRepository<Referido, Long> {

	boolean existsByActivoId(Long activoId);

	/** Historial completo con su activo, por fecha descendente y, a igual fecha, por id descendente. */
	@Query("""
			SELECT r FROM Referido r JOIN FETCH r.activo
			ORDER BY r.fechaReferido DESC, r.id DESC
			""")
	List<Referido> listarConActivo();

	@Query("SELECT r FROM Referido r JOIN FETCH r.activo WHERE r.id = :id")
	Optional<Referido> buscarConActivo(@Param("id") Long id);

}
