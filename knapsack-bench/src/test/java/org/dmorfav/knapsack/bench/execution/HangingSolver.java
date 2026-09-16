package org.dmorfav.knapsack.bench.execution;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;

/** Solver de prueba: nunca evalúa ni termina, así que solo lo para el límite duro del ejecutor aislado. */
public final class HangingSolver implements KnapsackSolver {

    @Override
    public SolverInfo info() {
        return new SolverInfo("test", "hanging", "Test");
    }

    @Override
    public Solution solve(Instance instance, SolverContext context) {
        while (true) {
            // bucle sin evaluar: el presupuesto no lo detiene
        }
    }
}
