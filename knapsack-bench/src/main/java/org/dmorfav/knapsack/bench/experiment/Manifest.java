package org.dmorfav.knapsack.bench.experiment;

import java.util.List;

import org.dmorfav.knapsack.core.solver.Budget;

/** manifest.json: todo lo necesario para interpretar y repetir un experimento. */
public record Manifest(String experiment, String description, String startedAt, String finishedAt, int runs,
                       long firstSeed, int warmupRuns, Long maxEvaluations, long maxSeconds, String runOrder,
                       String execution, int totalRuns, List<InstanceEntry> instances, List<SolverEntry> solvers,
                       EnvironmentInfo environment) {

    public record InstanceEntry(String name, String file, String sha256, int items, long capacity, long reference,
                                String referenceKind) {
    }

    public record SolverEntry(String id, String family, String variant, String author) {
    }

    /** {@code execution} describe cómo se aislaron las ejecuciones; los manifiestos antiguos sin el campo leen null. */
    static Manifest of(ExperimentPlan plan, String startedAt, String finishedAt, EnvironmentInfo environment,
                       String execution) {
        Experiment experiment = plan.experiment();
        Budget budget = experiment.budget();
        List<InstanceEntry> instances = plan.instances().stream()
                .map(planned -> new InstanceEntry(planned.instance().name(), planned.file(), planned.sha256(),
                        planned.instance().size(), planned.instance().capacity(), planned.reference().value(),
                        planned.reference().kind().name()))
                .toList();
        List<SolverEntry> solvers = plan.solvers().stream()
                .map(solver -> new SolverEntry(solver.info().id(), solver.info().family(), solver.info().variant(),
                        solver.info().author()))
                .toList();
        return new Manifest(experiment.name(), experiment.description(), startedAt, finishedAt, experiment.runs(),
                experiment.firstSeed(), experiment.warmupRuns(),
                budget.limitsEvaluations() ? budget.maxEvaluations() : null, budget.maxTime().toSeconds(),
                "barajado con semilla " + experiment.firstSeed(), execution, plan.tasks().size(), instances, solvers,
                environment);
    }
}
