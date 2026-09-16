package org.dmorfav.knapsack.baselines;

import org.dmorfav.knapsack.core.Evaluation;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;

final class BruteForce {

    private BruteForce() {
    }

    static long optimumProfit(Instance instance) {
        if (instance.size() > 20) {
            throw new IllegalArgumentException("fuerza bruta solo hasta 20 objetos");
        }
        long best = 0;
        for (int mask = 0; mask < 1 << instance.size(); mask++) {
            boolean[] selected = new boolean[instance.size()];
            for (int i = 0; i < instance.size(); i++) {
                selected[i] = (mask & (1 << i)) != 0;
            }
            Evaluation evaluation = instance.evaluate(Solution.of(selected));
            if (evaluation.feasible()) {
                best = Math.max(best, evaluation.profit());
            }
        }
        return best;
    }
}
