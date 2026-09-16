package org.dmorfav.knapsack.core.solver;

import java.time.Duration;

public final class BudgetExhaustedException extends RuntimeException {

    public BudgetExhaustedException(Budget budget, long evaluations, Duration elapsed) {
        super("presupuesto agotado tras " + evaluations + " evaluaciones y " + elapsed.toMillis() + " ms"
                + " (límite: " + describe(budget) + ")");
    }

    private static String describe(Budget budget) {
        String evaluations = budget.limitsEvaluations() ? budget.maxEvaluations() + " evaluaciones" : null;
        String time = budget.limitsTime() ? budget.maxTime().toMillis() + " ms" : null;
        if (evaluations != null && time != null) {
            return evaluations + ", " + time;
        }
        return evaluations != null ? evaluations : time;
    }
}
