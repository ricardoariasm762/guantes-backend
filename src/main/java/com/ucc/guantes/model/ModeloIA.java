package com.ucc.guantes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que registra las versiones de los modelos de IA (TensorFlow.js)
 * entrenados y desplegados para la evaluación de la técnica de postura de guantes.
 */
@Entity
@Table(name = "modelos_ia")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModeloIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String version;

    @Column(name = "accuracy", nullable = false)
    private Double accuracy;

    @Column(name = "num_clases", nullable = false)
    private Integer numClases;

    @Column(name = "num_muestras", nullable = false)
    private Integer numMuestras;

    @Column(name = "fecha_entrenamiento", nullable = false)
    private LocalDateTime fechaEntrenamiento;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "activo")
    @Builder.Default
    private Boolean activo = false;

    @PrePersist
    public void prePersist() {
        if (this.fechaEntrenamiento == null) {
            this.fechaEntrenamiento = LocalDateTime.now();
        }
        if (this.activo == null) {
            this.activo = false;
        }
    }
}
