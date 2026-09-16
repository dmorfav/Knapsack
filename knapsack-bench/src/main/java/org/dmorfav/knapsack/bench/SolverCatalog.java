package org.dmorfav.knapsack.bench;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import org.dmorfav.knapsack.core.solver.KnapsackSolver;

public final class SolverCatalog {

    private SolverCatalog() {
    }

    /** Todos los solvers registrados en META-INF/services del classpath, ordenados por id. */
    public static List<KnapsackSolver> discover() {
        return ServiceLoader.load(KnapsackSolver.class).stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparing(solver -> solver.info().id()))
                .toList();
    }
}
