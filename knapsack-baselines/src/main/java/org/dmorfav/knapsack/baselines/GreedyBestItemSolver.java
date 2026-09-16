package org.dmorfav.knapsack.baselines;

import org.dmorfav.knapsack.core.Evaluation;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;

/** El mejor entre el voraz y el objeto más valioso que cabe solo: garantiza al menos la mitad del óptimo. */
public final class GreedyBestItemSolver implements KnapsackSolver {

    private static final SolverInfo INFO = new SolverInfo("greedy", "ratio-best-item", "baseline");

    @Override
    public SolverInfo info() {
        return INFO;
    }

    @Override
    public Solution solve(Instance instance, SolverContext context) {
        Solution greedy = GreedySolver.fill(instance);
        Solution bestItem = bestSingleItem(instance);
        Evaluation greedyEvaluation = context.evaluate(greedy);
        Evaluation bestItemEvaluation = context.evaluate(bestItem);
        return bestItemEvaluation.profit() > greedyEvaluation.profit() ? bestItem : greedy;
    }

    static Solution bestSingleItem(Instance instance) {
        Solution solution = Solution.empty(instance.size());
        int best = -1;
        for (int i = 0; i < instance.size(); i++) {
            boolean fits = instance.item(i).weight() <= instance.capacity();
            if (fits && (best < 0 || instance.item(i).profit() > instance.item(best).profit())) {
                best = i;
            }
        }
        return best < 0 ? solution : solution.with(best, true);
    }
}
