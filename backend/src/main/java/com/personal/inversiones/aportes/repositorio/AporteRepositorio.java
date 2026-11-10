package com.personal.inversiones.aportes.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.personal.inversiones.aportes.modelo.Aporte;

public interface AporteRepositorio extends JpaRepository<Aporte, Long> {

	List<Aporte> findAllByOrderByFechaAporteDescIdDesc();

}
