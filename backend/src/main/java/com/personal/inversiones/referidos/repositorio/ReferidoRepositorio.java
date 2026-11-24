package com.personal.inversiones.referidos.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;

import com.personal.inversiones.referidos.modelo.Referido;

public interface ReferidoRepositorio extends JpaRepository<Referido, Long> {

}
