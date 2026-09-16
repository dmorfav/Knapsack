package org.dmorfav.knapsack.bench;

import org.dmorfav.knapsack.core.solver.Budget;

public final class BudgetText {

    private BudgetText() {
    }

    public static String describe(Budget budget) {
        String evaluations = budget.limitsEvaluations() ? budget.maxEvaluations() + " evaluaciones" : "evaluaciones sin límite";
        String time = budget.limitsTime() ? budget.maxTime().toSeconds() + " s" : "tiempo sin límite";
        return evaluations + ", " + time;
    }
}
