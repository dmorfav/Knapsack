package org.dmorfav.knapsack.bench.execution;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;

/** Solver de prueba: tarda en construirse más que el límite duro, como una JVM que arranca en frío en una máquina lenta. */
public final class SlowStartingSolver implements KnapsackSolver {

    static final long STARTUP_MILLIS = 2000;

    public SlowStartingSolver() throws InterruptedException {
        Thread.sleep(STARTUP_MILLIS);
    }

    @Override
    public SolverInfo info() {
        return new SolverInfo("test", "slow-starting", "Test");
    }

    @Override
    public Solution solve(Instance instance, SolverContext context) {
        return Solution.empty(instance.size());
    }
}
