package com.ucc.guantes.repository;

import com.ucc.guantes.model.IntentoPractica;
import com.ucc.guantes.model.ModoPractica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IntentoPracticaRepository extends JpaRepository<IntentoPractica, Long> {

    List<IntentoPractica> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);

    List<IntentoPractica> findByUsuarioIdAndModoPractica(Long usuarioId, ModoPractica modoPractica);

    Long countByUsuarioId(Long usuarioId);

    Long countByUsuarioIdAndCompletadoTrue(Long usuarioId);

    @org.springframework.data.jpa.repository.Query("SELECT AVG(i.confianzaPromedio) FROM IntentoPractica i WHERE i.confianzaPromedio IS NOT NULL")
    Double findPromedioConfianzaGlobal();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(i) FROM IntentoPractica i WHERE i.confianzaPromedio IS NOT NULL OR i.versionModelo IS NOT NULL")
    Long countIntentosConIA();
}
