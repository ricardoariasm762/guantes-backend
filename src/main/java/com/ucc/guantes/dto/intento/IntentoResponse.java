package com.ucc.guantes.dto.intento;

import com.ucc.guantes.model.ModoPractica;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntentoResponse {

    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private ModoPractica modoPractica;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Double puntaje;
    private Integer duracionSegundos;
    private Boolean completado;
    private String observaciones;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Confianza promedio del modelo de IA", example = "0.85")
    private Double confianzaPromedio;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Fuente principal de la predicción clínica", example = "CONSENSO")
    private com.ucc.guantes.model.FuentePrediccion fuentePrediccion;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Identificador de la versión del modelo de IA utilizado", example = "v1.0-2026-10")
    private String versionModelo;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Detalle serializado en formato JSON de la inferencia por paso")
    private String detalleIA;

    private List<PasoResponse> pasos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasoResponse {
        private Long id;
        private Integer numeroPaso;
        private String nombrePaso;
        private String descripcion;
        private Boolean exitoso;
        private Integer tiempoSegundos;
        private Double precision;
        private String observacionesError;
        private LocalDateTime fechaHora;
    }
}
