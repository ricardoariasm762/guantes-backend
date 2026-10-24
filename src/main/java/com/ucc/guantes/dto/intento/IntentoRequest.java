package com.ucc.guantes.dto.intento;

import com.ucc.guantes.model.ModoPractica;
import jakarta.validation.constraints.NotNull;
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
public class IntentoRequest {

    private Long usuarioId;

    @NotNull(message = "El modo de práctica es obligatorio")
    private ModoPractica modoPractica;

    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Double puntaje;
    private Integer duracionSegundos;
    private Boolean completado;
    private String observaciones;

    @jakarta.validation.constraints.DecimalMin(value = "0.0", message = "La confianza promedio no puede ser menor a 0.0")
    @jakarta.validation.constraints.DecimalMax(value = "1.0", message = "La confianza promedio no puede ser mayor a 1.0")
    @io.swagger.v3.oas.annotations.media.Schema(description = "Confianza promedio del modelo de IA (entre 0.0 y 1.0)", example = "0.85")
    private Double confianzaPromedio;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Fuente principal de decisión en la evaluación", example = "CONSENSO")
    private com.ucc.guantes.model.FuentePrediccion fuentePrediccion;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Identificador de la versión del modelo de IA utilizado", example = "v1.0-2026-10")
    private String versionModelo;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Detalle serializado en formato JSON de landmarks y confianzas por paso")
    private String detalleIA;

    private List<PasoRequest> pasos;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasoRequest {
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
