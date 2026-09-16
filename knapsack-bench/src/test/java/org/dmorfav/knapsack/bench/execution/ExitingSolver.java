package org.dmorfav.knapsack.bench.execution;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;

/** Solver de prueba: tumba su propia JVM, como haría un System.exit en un algoritmo de un colaborador. */
public final class ExitingSolver implements KnapsackSolver {

    @Override
    public SolverInfo info() {
        return new SolverInfo("test", "exiting", "Test");
    }

    @Override
    public Solution solve(Instance instance, SolverContext context) {
        Runtime.getRuntime().halt(3);
        return Solution.empty(instance.size());
    }
}
