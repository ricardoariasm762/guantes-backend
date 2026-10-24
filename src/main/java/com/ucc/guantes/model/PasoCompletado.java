package com.ucc.guantes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pasos_completados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasoCompletado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "intento_id", nullable = false)
    @JsonIgnore
    private IntentoPractica intentoPractica;

    @Column(name = "numero_paso", nullable = false)
    private Integer numeroPaso;

    @Column(name = "nombre_paso", nullable = false, length = 150)
    private String nombrePaso;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private Boolean exitoso = true;

    @Column(name = "tiempo_segundos")
    private Integer tiempoSegundos;

    @Column(name = "precision_obtenida")
    private Double precision;

    @Column(name = "observaciones_error", columnDefinition = "TEXT")
    private String observacionesError;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    @PrePersist
    public void prePersist() {
        if (fechaHora == null) {
            fechaHora = LocalDateTime.now();
        }
        if (exitoso == null) {
            exitoso = true;
        }
    }
}
