package com.ucc.guantes.dto.ia;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para la respuesta con información de una versión del modelo de IA.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Información detallada de una versión del modelo de IA")
public class ModeloIAResponse {

    @Schema(description = "Identificador único en base de datos", example = "1")
    private Long id;

    @Schema(description = "Identificador de versión del modelo", example = "v1.0-2026-10")
    private String version;

    @Schema(description = "Precisión global (accuracy) del modelo", example = "0.945")
    private Double accuracy;

    @Schema(description = "Número de clases clasificadas", example = "6")
    private Integer numClases;

    @Schema(description = "Número de muestras de entrenamiento", example = "15200")
    private Integer numMuestras;

    @Schema(description = "Fecha y hora en que se entrenó o registró el modelo")
    private LocalDateTime fechaEntrenamiento;

    @Schema(description = "Descripción o notas de la versión")
    private String descripcion;

    @Schema(description = "Indica si es el modelo actualmente activo en la plataforma", example = "true")
    private Boolean activo;
}
