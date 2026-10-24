package com.ucc.guantes.model;

/**
 * Enum que define el origen o mecanismo de decisión clínica utilizado
 * durante la evaluación del intento de postura de guantes quirúrgicos.
 */
public enum FuentePrediccion {
    /**
     * La decisión provino principalmente del modelo de machine learning (TensorFlow.js).
     */
    IA,

    /**
     * La decisión provino principalmente de la evaluación de reglas clínicas algorítmicas.
     */
    REGLAS,

    /**
     * Ambas fuentes (modelo de IA y reglas clínicas) coincidieron en la predicción.
     */
    CONSENSO,

    /**
     * Hubo conflicto o discrepancia entre IA y reglas clínicas, resuelto según heurística/prioridad.
     */
    MIXTO
}
