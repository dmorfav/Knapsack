package org.dmorfav.knapsack.bench.experiment;

import java.util.List;
import java.util.Map;

import org.dmorfav.knapsack.core.solver.Budget;

/** manifest.json: todo lo necesario para interpretar y repetir un experimento. */
public record Manifest(String experiment, String description, String startedAt, String finishedAt, int runs,
                       long firstSeed, int warmupRuns, Long maxEvaluations, long maxSeconds, String runOrder,
                       String execution, int totalRuns, List<InstanceEntry> instances, List<SolverEntry> solvers,
                       EnvironmentInfo environment) {

    public record InstanceEntry(String name, String file, String sha256, int items, long capacity, long reference,
                                String referenceKind) {
    }

    /** Los campos de procedencia (ver {@link SolverProvenance}) son null en manifiestos anteriores a ellos. */
    public record SolverEntry(String id, String family, String variant, String author, String codeHash,
                              List<String> sources, List<String> gitAuthors, Boolean uncommittedChanges) {
    }

    /** {@code execution} describe cómo se aislaron las ejecuciones; los manifiestos antiguos sin el campo leen null. */
    static Manifest of(ExperimentPlan plan, String startedAt, String finishedAt, EnvironmentInfo environment,
                       String execution, Map<String, SolverProvenance> provenance) {
        Experiment experiment = plan.experiment();
        Budget budget = experiment.budget();
        List<InstanceEntry> instances = plan.instances().stream()
                .map(planned -> new InstanceEntry(planned.instance().name(), planned.file(), planned.sha256(),
                        planned.instance().size(), planned.instance().capacity(), planned.reference().value(),
                        planned.reference().kind().name()))
                .toList();
        List<SolverEntry> solvers = plan.solvers().stream()
                .map(solver -> {
                    SolverProvenance origin = provenance.get(solver.info().id());
                    return new SolverEntry(solver.info().id(), solver.info().family(), solver.info().variant(),
                            solver.info().author(), origin.codeHash(), origin.sources(), origin.gitAuthors(),
                            origin.uncommittedChanges());
                })
                .toList();
        return new Manifest(experiment.name(), experiment.description(), startedAt, finishedAt, experiment.runs(),
                experiment.firstSeed(), experiment.warmupRuns(),
                budget.limitsEvaluations() ? budget.maxEvaluations() : null, budget.maxTime().toSeconds(),
                "barajado con semilla " + experiment.firstSeed(), execution, plan.tasks().size(), instances, solvers,
                environment);
    }
}
