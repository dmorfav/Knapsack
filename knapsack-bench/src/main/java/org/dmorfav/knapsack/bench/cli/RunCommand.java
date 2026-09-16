package org.dmorfav.knapsack.bench.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.dmorfav.knapsack.baselines.UpperBounds;
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
        long upperBound = UpperBounds.dantzig(instance);

        out.printf(Locale.ROOT, "Instancia %s: %d objetos, capacidad %d, %s%n", instance.name(), instance.size(),
                instance.capacity(), instance.knownOptimum().isPresent()
                        ? "óptimo " + instance.knownOptimum().getAsLong()
                        : "óptimo desconocido (cota de Dantzig " + upperBound + ")");
        out.printf(Locale.ROOT, "Semilla %d, presupuesto por solver: %s%n%n", seed, describe(budget));

        for (KnapsackSolver solver : solvers) {
            print(out, SolverRun.execute(solver, instance, budget, seed), instance, upperBound);
        }
        out.flush();
        return 0;
    }

    private Budget budget() {
        Budget budget = Budget.ofTime(Duration.ofSeconds(maxSeconds));
        return maxEvaluations == null ? budget : budget.withMaxEvaluations(maxEvaluations);
    }

    private static void print(PrintWriter out, SolverRun run, Instance instance, long upperBound) {
        out.printf(Locale.ROOT, "%s (%s)%n", run.info().id(), run.info().author());
        if (run.evaluation() != null) {
            out.printf(Locale.ROOT, "  %s%n  beneficio %d · peso %d/%d · %d objetos · %s%n",
                    run.solution(), run.evaluation().profit(), run.evaluation().weight(), instance.capacity(),
                    run.solution().selectedCount(), gap(run.evaluation().profit(), instance, upperBound));
        }
        out.printf(Locale.ROOT, "  %d evaluaciones · %.2f ms%n", run.evaluations(), run.elapsed().toNanos() / 1_000_000.0);
        if (!run.succeeded()) {
            out.printf("  FALLO: %s%n", run.failure());
        }
        out.println();
    }

    private static String gap(long profit, Instance instance, long upperBound) {
        if (instance.knownOptimum().isPresent()) {
            return String.format(Locale.ROOT, "gap %.2f %%", percentBelow(profit, instance.knownOptimum().getAsLong()));
        }
        return String.format(Locale.ROOT, "gap ≤ %.2f %% (cota)", percentBelow(profit, upperBound));
    }

    private static double percentBelow(long profit, long reference) {
        return reference == 0 ? 0 : 100.0 * (reference - profit) / reference;
    }

    private static String describe(Budget budget) {
        String evaluations = budget.limitsEvaluations() ? budget.maxEvaluations() + " evaluaciones" : "evaluaciones sin límite";
        String time = budget.limitsTime() ? budget.maxTime().toSeconds() + " s" : "tiempo sin límite";
        return evaluations + ", " + time;
    }
}
