package com.ucc.guantes.dto.progreso;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgresoResponse {

    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private Integer intentosTotales;
    private Integer intentosExitosos;
    private Integer tiempoTotalSegundos;
    private Double promedioPuntaje;
    private Double mejorPuntaje;
    private String nivelAlcanzado;
    private LocalDateTime ultimoAcceso;
}
