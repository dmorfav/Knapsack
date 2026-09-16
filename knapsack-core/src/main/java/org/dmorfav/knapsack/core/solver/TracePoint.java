package org.dmorfav.knapsack.core.solver;

/** Mejora del mejor beneficio factible evaluado: en qué evaluación y en qué instante de la ejecución ocurrió. */
public record TracePoint(long evaluation, long elapsedNanos, long profit) {
}
