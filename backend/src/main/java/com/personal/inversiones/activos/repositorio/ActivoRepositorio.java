package com.personal.inversiones.activos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.personal.inversiones.activos.modelo.Activo;

public interface ActivoRepositorio extends JpaRepository<Activo, Long> {
}
