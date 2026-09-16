package org.dmorfav.knapsack.bench.experiment;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.dmorfav.knapsack.bench.BudgetText;
import org.dmorfav.knapsack.bench.SolverRun;
import org.dmorfav.knapsack.bench.stats.Comparison;
import org.dmorfav.knapsack.bench.stats.Summarizer;
import org.dmorfav.knapsack.bench.stats.SummaryOutput;
import org.dmorfav.knapsack.bench.stats.SummaryRow;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;

/**
 * Ejecuta un experimento de forma secuencial en esta JVM: calentamiento por solver, tareas en orden barajado y una
 * línea de resultados por ejecución.
 */
public final class ExperimentRunner {

    private static final DateTimeFormatter DIRECTORY_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final List<KnapsackSolver> availableSolvers;
    private final PrintWriter out;
    private final Clock clock;

    public ExperimentRunner(List<KnapsackSolver> availableSolvers, PrintWriter out, Clock clock) {
        this.availableSolvers = availableSolvers;
        this.out = out;
        this.clock = clock;
    }

    /** Resuelve las rutas de instancias desde {@code root} y escribe en {@code outputBase/<nombre>/<marca UTC>}. */
    public ResultsDirectory run(Experiment experiment, Path root, Path outputBase) throws IOException {
        ExperimentPlan plan = ExperimentPlan.resolve(experiment, root, availableSolvers);
        Instant startedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        ResultsDirectory results = new ResultsDirectory(createDirectory(outputBase.resolve(experiment.name()), startedAt));
        EnvironmentInfo environment = EnvironmentInfo.capture(root);
        results.writeManifest(Manifest.of(plan, startedAt.toString(), null, environment));

        out.printf(Locale.ROOT, "Experimento %s: %d instancias × %d solvers × %d ejecuciones = %d%n",
                experiment.name(), plan.instances().size(), plan.solvers().size(), experiment.runs(), plan.tasks().size());
        out.printf(Locale.ROOT, "Presupuesto por ejecución: %s · resultados en %s%n",
                BudgetText.describe(experiment.budget()), results.path());
        out.flush();

        warmUp(plan);
        try (ResultsDirectory.JsonLinesWriter runs = results.openJsonLines(ResultsDirectory.RUNS);
             ResultsDirectory.JsonLinesWriter traces = results.openJsonLines(ResultsDirectory.TRACES)) {
            int done = 0;
            for (ExperimentPlan.Task task : plan.tasks()) {
                SolverRun run = SolverRun.execute(task.solver(), task.instance().instance(), experiment.budget(), task.seed());
                RunRecord record = RunRecord.of(task.instance(), run);
                runs.append(record);
                traces.append(TraceRecord.of(task.instance(), run));
                done++;
                printProgress(done, plan.tasks().size(), record);
            }
        }

        results.writeManifest(Manifest.of(plan, startedAt.toString(),
                clock.instant().truncatedTo(ChronoUnit.SECONDS).toString(), environment));
        List<RunRecord> records = results.readRuns();
        List<SummaryRow> summary = Summarizer.summarize(records);
        SummaryOutput.writeCsv(summary, results.path().resolve(ResultsDirectory.SUMMARY));
        SummaryOutput.printTable(summary, out);
        SummaryOutput.printComparison(Comparison.of(records), out);
        return results;
    }

    private void warmUp(ExperimentPlan plan) {
        Experiment experiment = plan.experiment();
        if (experiment.warmupRuns() == 0) {
            return;
        }
        ExperimentPlan.PlannedInstance first = plan.instances().getFirst();
        for (KnapsackSolver solver : plan.solvers()) {
            for (int i = 0; i < experiment.warmupRuns(); i++) {
                SolverRun.execute(solver, first.instance(), experiment.budget(), experiment.firstSeed() - 1 - i);
            }
        }
    }

    private void printProgress(int done, int total, RunRecord record) {
        String result = record.succeeded()
                ? String.format(Locale.ROOT, "beneficio %d, gap %s%.2f%%", record.profit(),
                "OPTIMUM".equals(record.referenceKind()) ? "" : "≤", record.gap())
                : "FALLO: " + record.failure();
        out.printf(Locale.ROOT, "[%d/%d] %s · %s · semilla %d · %s%n",
                done, total, record.instance(), record.solver(), record.seed(), result);
        out.flush();
    }

    private static Path createDirectory(Path experimentDirectory, Instant startedAt) throws IOException {
        String base = DIRECTORY_TIMESTAMP.format(startedAt);
        Path candidate = experimentDirectory.resolve(base);
        for (int suffix = 2; Files.exists(candidate); suffix++) {
            candidate = experimentDirectory.resolve(base + "-" + suffix);
        }
        return Files.createDirectories(candidate);
    }
}
