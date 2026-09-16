package org.dmorfav.knapsack.bench.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.dmorfav.knapsack.bench.BudgetText;
import org.dmorfav.knapsack.bench.Reference;
import org.dmorfav.knapsack.bench.SolverCatalog;
import org.dmorfav.knapsack.bench.SolverRun;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.io.InstanceFiles;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "run", mixinStandardHelpOptions = true,
        description = "Ejecuta una vez cada solver registrado sobre una instancia y muestra sus resultados.")
public final class RunCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = "--instance", paramLabel = "<ruta>", defaultValue = "instances/legacy-20.json",
            description = "Instancia JSON (por defecto: ${DEFAULT-VALUE}).")
    Path instancePath;

    @Option(names = "--seed", defaultValue = "1", description = "Semilla de la ejecución (por defecto: ${DEFAULT-VALUE}).")
    long seed;

    @Option(names = "--max-evaluations", paramLabel = "<n>",
            description = "Evaluaciones máximas por solver (por defecto sin límite).")
    Long maxEvaluations;

    @Option(names = "--max-seconds", paramLabel = "<n>", defaultValue = "60",
            description = "Segundos máximos por solver (por defecto: ${DEFAULT-VALUE}).")
    long maxSeconds;

    @Override
    public Integer call() throws IOException {
        PrintWriter out = spec.commandLine().getOut();
        Instance instance = InstanceFiles.read(instancePath);
        Budget budget = budget();
        List<KnapsackSolver> solvers = SolverCatalog.discover();
        Reference reference = Reference.of(instance);

        out.printf(Locale.ROOT, "Instancia %s: %d objetos, capacidad %d, %s%n", instance.name(), instance.size(),
                instance.capacity(), reference.kind() == Reference.Kind.OPTIMUM
                        ? "óptimo " + reference.value()
                        : "óptimo desconocido (cota de Dantzig " + reference.value() + ")");
        out.printf(Locale.ROOT, "Semilla %d, presupuesto por solver: %s%n%n", seed, BudgetText.describe(budget));

        for (KnapsackSolver solver : solvers) {
            print(out, SolverRun.execute(solver, instance, budget, seed), instance, reference);
        }
        out.flush();
        return 0;
    }

    private Budget budget() {
        Budget budget = Budget.ofTime(Duration.ofSeconds(maxSeconds));
        return maxEvaluations == null ? budget : budget.withMaxEvaluations(maxEvaluations);
    }

    private static void print(PrintWriter out, SolverRun run, Instance instance, Reference reference) {
        out.printf(Locale.ROOT, "%s (%s)%n", run.info().id(), run.info().author());
        if (run.evaluation() != null) {
            long profit = run.evaluation().profit();
            String gap = String.format(Locale.ROOT, reference.kind() == Reference.Kind.OPTIMUM ? "gap %.2f %%" : "gap ≤ %.2f %% (cota)",
                    reference.gapPercent(profit));
            out.printf(Locale.ROOT, "  %s%n  beneficio %d · peso %d/%d · %d objetos · %s%n",
                    run.solution(), profit, run.evaluation().weight(), instance.capacity(),
                    run.solution().selectedCount(), gap);
        }
        out.printf(Locale.ROOT, "  %d evaluaciones · %.2f ms%n", run.evaluations(), run.elapsed().toNanos() / 1_000_000.0);
        if (!run.succeeded()) {
            out.printf("  FALLO: %s%n", run.failure());
        }
        out.println();
    }
}
