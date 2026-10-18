package org.dmorfav.knapsack.bench.execution;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;

/** Solver de prueba: se construye en el banco, pero su constructor falla dentro del proceso aislado, antes de estar listo. */
public final class FailingConstructorSolver implements KnapsackSolver {

    public FailingConstructorSolver() {
        boolean insideWorker = StackWalker.getInstance()
                .walk(frames -> frames.anyMatch(frame -> frame.getClassName().equals(SolverWorker.class.getName())));
        if (insideWorker) {
            throw new IllegalStateException("no arranca");
        }
    }

    @Override
    public SolverInfo info() {
        return new SolverInfo("test", "failing-constructor", "Test");
    }

    @Override
    public Solution solve(Instance instance, SolverContext context) {
        return Solution.empty(instance.size());
    }
}
