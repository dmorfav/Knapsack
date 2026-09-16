package org.dmorfav.knapsack.baselines;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;

/** Recorre los objetos por ratio beneficio/peso decreciente y mete cada uno que todavía quepa. */
public final class GreedySolver implements KnapsackSolver {

    private static final SolverInfo INFO = new SolverInfo("greedy", "ratio", "baseline");

    @Override
    public SolverInfo info() {
        return INFO;
    }

    @Override
    public Solution solve(Instance instance, SolverContext context) {
        Solution solution = fill(instance);
        context.evaluate(solution);
        return solution;
    }

    static Solution fill(Instance instance) {
        boolean[] selected = new boolean[instance.size()];
        long remaining = instance.capacity();
        for (int index : RatioOrder.byDecreasingRatio(instance)) {
            long weight = instance.item(index).weight();
            if (weight <= remaining) {
                selected[index] = true;
                remaining -= weight;
            }
        }
        return Solution.of(selected);
    }
}
