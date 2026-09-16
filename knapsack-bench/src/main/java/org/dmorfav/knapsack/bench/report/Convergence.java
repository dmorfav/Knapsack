package org.dmorfav.knapsack.bench.report;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.dmorfav.knapsack.bench.stats.Descriptive;

/** Curva mediana de convergencia de un solver: GAP del mejor valor encontrado hasta cada número de evaluaciones. */
final class Convergence {

    static final int CHECKPOINTS = 40;

    record Point(long evaluations, double gap, int runs) {
    }

    private Convergence() {
    }

    /**
     * {@code traces} contiene, por ejecución, los puntos [evaluación, nanos, beneficio] en orden. En cada punto de
     * control (espaciados logarítmicamente) se toma la mediana del GAP de todas las ejecuciones. Una ejecución que aún
     * no encontró nada factible vale como la mochila vacía (beneficio 0), así que la curva nunca sube.
     */
    static List<Point> medianCurve(List<List<long[]>> traces, long reference) {
        long maxEvaluations = traces.stream()
                .filter(trace -> !trace.isEmpty())
                .mapToLong(trace -> trace.getLast()[0])
                .max()
                .orElse(0);
        if (maxEvaluations == 0) {
            return List.of();
        }
        List<Point> curve = new ArrayList<>();
        for (long checkpoint : checkpoints(maxEvaluations)) {
            double[] gaps = traces.stream()
                    .mapToLong(trace -> bestProfitUpTo(trace, checkpoint))
                    .mapToDouble(profit -> reference == 0 ? 0 : 100.0 * (reference - profit) / reference)
                    .toArray();
            curve.add(new Point(checkpoint, Descriptive.median(gaps), gaps.length));
        }
        return curve;
    }

    static List<Long> checkpoints(long maxEvaluations) {
        TreeSet<Long> points = new TreeSet<>();
        double logMax = Math.log(maxEvaluations);
        for (int i = 0; i < CHECKPOINTS; i++) {
            points.add(Math.max(1, Math.round(Math.exp(logMax * i / (CHECKPOINTS - 1)))));
        }
        points.add(maxEvaluations);
        return List.copyOf(points);
    }

    private static long bestProfitUpTo(List<long[]> trace, long evaluations) {
        long best = 0;
        for (long[] point : trace) {
            if (point[0] > evaluations) {
                break;
            }
            best = point[2];
        }
        return best;
    }
}
