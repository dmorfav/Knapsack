package org.dmorfav.knapsack.bench.experiment;

import org.dmorfav.knapsack.bench.SolverRun;
import org.dmorfav.knapsack.core.solver.TracePoint;

/**
 * Una línea de runs.jsonl: una ejecución de un solver sobre una instancia con una semilla. Los campos de resultado
 * son null cuando {@code status} es "failed". {@code gap} es un tanto por ciento respecto a {@code reference}.
 */
public record RunRecord(String instance, String instanceFile, String solver, String family, String variant,
                        String author, long seed, String status, String failure, Long profit, Long weight,
                        Integer selected, long evaluations, long elapsedNanos, long reference, String referenceKind,
                        Double gap, Long evaluationsToBest, Long nanosToBest, String solution) {

    public static final String OK = "ok";
    public static final String FAILED = "failed";

    static RunRecord of(ExperimentPlan.PlannedInstance planned, SolverRun run) {
        TracePoint reach = run.firstReachOfFinalProfit();
        boolean ok = run.succeeded();
        return new RunRecord(
                planned.instance().name(), planned.file(),
                run.info().id(), run.info().family(), run.info().variant(), run.info().author(),
                run.seed(), ok ? OK : FAILED, run.failure(),
                ok ? run.evaluation().profit() : null,
                ok ? run.evaluation().weight() : null,
                ok ? run.solution().selectedCount() : null,
                run.evaluations(), run.elapsed().toNanos(),
                planned.reference().value(), planned.reference().kind().name(),
                ok ? planned.reference().gapPercent(run.evaluation().profit()) : null,
                ok && reach != null ? reach.evaluation() : null,
                ok && reach != null ? reach.elapsedNanos() : null,
                ok ? run.solution().toString() : null);
    }

    public boolean succeeded() {
        return OK.equals(status);
    }

    public boolean reachedOptimum() {
        return succeeded() && "OPTIMUM".equals(referenceKind) && profit == reference;
    }
}
