package com.ucc.guantes.repository;

import com.ucc.guantes.model.PasoCompletado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PasoCompletadoRepository extends JpaRepository<PasoCompletado, Long> {

    List<PasoCompletado> findByIntentoPracticaIdOrderByNumeroPasoAsc(Long intentoId);
}
