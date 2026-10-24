package com.ucc.guantes.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "intentos_practica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntentoPractica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_practica", nullable = false, length = 30)
    private ModoPractica modoPractica;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "puntaje")
    private Double puntaje;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completado = false;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "confianza_promedio")
    private Double confianzaPromedio;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente_prediccion", length = 20)
    private FuentePrediccion fuentePrediccion;

    @Column(name = "version_modelo", length = 50)
    private String versionModelo;

    @Column(name = "detalle_ia", columnDefinition = "TEXT")
    private String detalleIA;

    @OneToMany(mappedBy = "intentoPractica", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PasoCompletado> pasos = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaInicio == null) {
            fechaInicio = LocalDateTime.now();
        }
        if (completado == null) {
            completado = false;
        }
    }

    public void addPaso(PasoCompletado paso) {
        pasos.add(paso);
        paso.setIntentoPractica(this);
    }

    public void removePaso(PasoCompletado paso) {
        pasos.remove(paso);
        paso.setIntentoPractica(null);
    }
}
