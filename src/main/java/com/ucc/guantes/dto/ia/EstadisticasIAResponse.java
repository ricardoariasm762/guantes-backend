package com.ucc.guantes.dto.ia;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para las métricas e indicadores agregados sobre el uso del modelo de IA en las prácticas.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Métricas agregadas sobre el uso y rendimiento de la IA en los intentos de práctica")
public class EstadisticasIAResponse {

    @Schema(description = "Confianza promedio global de todos los intentos evaluados con IA (0.0 a 1.0)", example = "0.865")
    private Double confianzaPromedioGlobal;

    @Schema(description = "Número total de intentos evaluados con inferencia de IA", example = "142")
    private Long totalIntentosConIA;

    @Schema(description = "Número total acumulado de intentos registrados en la plataforma", example = "180")
    private Long totalIntentosTotales;

    @Schema(description = "Tasa porcentual o proporción de intentos evaluados con IA (0.0 a 1.0)", example = "0.789")
    private Double tasaUsoIA;
}
