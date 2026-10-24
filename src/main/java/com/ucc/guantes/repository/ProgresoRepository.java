package com.ucc.guantes.repository;

import com.ucc.guantes.model.Progreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProgresoRepository extends JpaRepository<Progreso, Long> {

    Optional<Progreso> findByUsuarioId(Long usuarioId);

    Optional<Progreso> findByUsuarioEmail(String email);
}
