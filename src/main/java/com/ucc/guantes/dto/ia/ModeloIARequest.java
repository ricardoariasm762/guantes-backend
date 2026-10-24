package com.ucc.guantes.dto.ia;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de registro o actualización de versiones del modelo de IA.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Datos para el registro de una versión del modelo de inteligencia artificial")
public class ModeloIARequest {

    @NotBlank(message = "La versión del modelo es obligatoria")
    @Size(max = 50, message = "La versión no puede exceder 50 caracteres")
    @Schema(description = "Identificador único de la versión del modelo", example = "v1.0-2026-10")
    private String version;

    @NotNull(message = "El accuracy es obligatorio")
    @DecimalMin(value = "0.0", message = "El accuracy mínimo permitido es 0.0")
    @DecimalMax(value = "1.0", message = "El accuracy máximo permitido es 1.0")
    @Schema(description = "Precisión global alcanzada por el modelo (0.0 a 1.0)", example = "0.945")
    private Double accuracy;

    @NotNull(message = "El número de clases es obligatorio")
    @Min(value = 1, message = "El modelo debe clasificar al menos 1 clase")
    @Schema(description = "Número total de clases/etapas de postura clasificadas", example = "6")
    private Integer numClases;

    @NotNull(message = "El número de muestras es obligatorio")
    @Min(value = 1, message = "Debe haber al menos 1 muestra de entrenamiento")
    @Schema(description = "Cantidad de muestras de landmarks utilizadas en el entrenamiento", example = "15200")
    private Integer numMuestras;

    @Schema(description = "Descripción técnica, hiperparámetros o notas de la versión", example = "Modelo entrenado con TensorFlow.js para clasificación de 6 fases quirúrgicas")
    private String descripcion;

    @Schema(description = "Indica si esta versión debe ser establecida inmediatamente como la activa", example = "true")
    private Boolean activo;
}
