package com.personal.inversiones.transacciones.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.personal.inversiones.transacciones.modelo.Transaccion;

public interface TransaccionRepositorio extends JpaRepository<Transaccion, Long> {

	boolean existsByActivoId(Long activoId);

	List<Transaccion> findByActivoId(Long activoId);

	/** Historial completo con su activo, por fecha descendente y, a igual fecha, por id descendente. */
	@Query("""
			SELECT t FROM Transaccion t JOIN FETCH t.activo
			ORDER BY t.fechaTransaccion DESC, t.id DESC
			""")
	List<Transaccion> listarConActivo();

	/** Historial de un activo, con el mismo orden que {@link #listarConActivo()}. */
	@Query("""
			SELECT t FROM Transaccion t JOIN FETCH t.activo a
			WHERE a.id = :activoId
			ORDER BY t.fechaTransaccion DESC, t.id DESC
			""")
	List<Transaccion> listarConActivoPorActivoId(@Param("activoId") Long activoId);

	@Query("SELECT t FROM Transaccion t JOIN FETCH t.activo WHERE t.id = :id")
	Optional<Transaccion> buscarConActivo(@Param("id") Long id);

}
