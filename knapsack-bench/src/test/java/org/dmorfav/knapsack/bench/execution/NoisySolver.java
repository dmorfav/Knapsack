package org.dmorfav.knapsack.bench.execution;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;

/** Solver de prueba: imprime por la salida estándar, que el worker redirige para no corromper el protocolo. */
public final class NoisySolver implements KnapsackSolver {

    @Override
    public SolverInfo info() {
        return new SolverInfo("test", "noisy", "Test");
    }

    @Override
    public Solution solve(Instance instance, SolverContext context) {
        System.out.println("ruido que no debe llegar al canal del protocolo");
        Solution solution = Solution.empty(instance.size());
        context.evaluate(solution);
        return solution;
    }
}
