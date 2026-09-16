package org.dmorfav.knapsack.bench.stats;

/**
 * Agregado de las ejecuciones de un solver sobre una instancia. Las métricas de calidad y esfuerzo se calculan solo
 * sobre las ejecuciones correctas y son null si no hubo ninguna. {@code optimumHits} es null si no se conoce el óptimo.
 */
public record SummaryRow(String instance, String solver, String author, int runs, int succeeded, long reference,
                         String referenceKind, Long bestProfit, Double meanProfit, Double medianProfit,
                         Double stdProfit, Long worstProfit, Double bestGap, Double meanGap, Double medianGap,
                         Integer optimumHits, Double medianEvaluations, Double medianEvaluationsToBest,
                         Double medianMillis) {
}
