package org.dmorfav.knapsack.bench.experiment;

import java.util.List;

import org.dmorfav.knapsack.bench.SolverRun;

/** Una línea de traces.jsonl: la curva de convergencia de una ejecución como [evaluación, nanos, beneficio]. */
public record TraceRecord(String instance, String solver, long seed, List<long[]> points) {

    static TraceRecord of(ExperimentPlan.PlannedInstance planned, SolverRun run) {
        List<long[]> points = run.trace().stream()
                .map(point -> new long[] {point.evaluation(), point.elapsedNanos(), point.profit()})
                .toList();
        return new TraceRecord(planned.instance().name(), run.info().id(), run.seed(), points);
    }
}
