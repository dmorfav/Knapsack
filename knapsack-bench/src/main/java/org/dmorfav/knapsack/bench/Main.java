package org.dmorfav.knapsack.bench;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.io.InstanceFiles;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println(Options.USAGE);
            System.exit(2);
            return;
        }

        Instance instance = InstanceFiles.read(options.instance());
        List<KnapsackSolver> solvers = discoverSolvers();
        System.out.printf(Locale.ROOT, "Instancia %s: %d objetos, capacidad %d%n",
                instance.name(), instance.size(), instance.capacity());
        System.out.printf(Locale.ROOT, "Semilla %d, presupuesto por algoritmo: %s%n%n",
                options.seed(), describe(options.budget()));

        for (KnapsackSolver solver : solvers) {
            print(SolverRun.execute(solver, instance, options.budget(), options.seed()), instance);
        }
    }

    static List<KnapsackSolver> discoverSolvers() {
        return ServiceLoader.load(KnapsackSolver.class).stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparing(solver -> solver.info().id()))
                .toList();
    }

    private static void print(SolverRun run, Instance instance) {
        System.out.printf(Locale.ROOT, "%s (%s)%n", run.info().id(), run.info().author());
        if (run.evaluation() != null) {
            System.out.printf(Locale.ROOT, "  %s%n  beneficio %d · peso %d/%d · %d objetos%n",
                    run.solution(), run.evaluation().profit(), run.evaluation().weight(), instance.capacity(),
                    run.solution().selectedCount());
        }
        System.out.printf(Locale.ROOT, "  %d evaluaciones · %.2f ms%n",
                run.evaluations(), run.elapsed().toNanos() / 1_000_000.0);
        if (!run.succeeded()) {
            System.out.printf("  FALLO: %s%n", run.failure());
        }
        System.out.println();
    }

    private static String describe(Budget budget) {
        String evaluations = budget.limitsEvaluations() ? budget.maxEvaluations() + " evaluaciones" : "evaluaciones sin límite";
        String time = budget.limitsTime() ? budget.maxTime().toSeconds() + " s" : "tiempo sin límite";
        return evaluations + ", " + time;
    }
}
