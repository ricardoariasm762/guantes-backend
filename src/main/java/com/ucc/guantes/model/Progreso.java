package com.ucc.guantes.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "progresos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Progreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "intentos_totales", nullable = false)
    @Builder.Default
    private Integer intentosTotales = 0;

    @Column(name = "intentos_exitosos", nullable = false)
    @Builder.Default
    private Integer intentosExitosos = 0;

    @Column(name = "tiempo_total_segundos", nullable = false)
    @Builder.Default
    private Integer tiempoTotalSegundos = 0;

    @Column(name = "promedio_puntaje", nullable = false)
    @Builder.Default
    private Double promedioPuntaje = 0.0;

    @Column(name = "mejor_puntaje", nullable = false)
    @Builder.Default
    private Double mejorPuntaje = 0.0;

    @Column(name = "nivel_alcanzado", length = 50)
    @Builder.Default
    private String nivelAlcanzado = "PRINCIPIANTE";

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @PrePersist
    public void prePersist() {
        if (intentosTotales == null) intentosTotales = 0;
        if (intentosExitosos == null) intentosExitosos = 0;
        if (tiempoTotalSegundos == null) tiempoTotalSegundos = 0;
        if (promedioPuntaje == null) promedioPuntaje = 0.0;
        if (mejorPuntaje == null) mejorPuntaje = 0.0;
        if (nivelAlcanzado == null) nivelAlcanzado = "PRINCIPIANTE";
        if (ultimoAcceso == null) ultimoAcceso = LocalDateTime.now();
    }
}
