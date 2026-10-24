package com.ucc.guantes.repository;

import com.ucc.guantes.model.ModeloIA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad ModeloIA.
 */
@Repository
public interface ModeloIARepository extends JpaRepository<ModeloIA, Long> {

    /**
     * Busca un modelo por su código o identificador de versión único.
     *
     * @param version Identificador de versión (ej: "v1.0-2026-10")
     * @return Optional con el modelo si existe
     */
    Optional<ModeloIA> findByVersion(String version);

    /**
     * Busca el modelo que se encuentra actualmente marcado como activo.
     *
     * @return Optional con el modelo activo si existe
     */
    Optional<ModeloIA> findByActivoTrue();

    /**
     * Verifica si ya existe registrado un modelo con la versión especificada.
     *
     * @param version Identificador de versión a validar
     * @return true si existe, false en caso contrario
     */
    boolean existsByVersion(String version);
}
