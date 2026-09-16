package org.dmorfav.knapsack.core.solver;

import java.time.Duration;
import java.util.Objects;

public record Budget(long maxEvaluations, Duration maxTime) {

    private static final Duration NO_TIME_LIMIT = Duration.ofNanos(Long.MAX_VALUE);

    public Budget {
        if (maxEvaluations <= 0) {
            throw new IllegalArgumentException("maxEvaluations debe ser > 0, es " + maxEvaluations);
        }
        Objects.requireNonNull(maxTime, "maxTime");
        if (maxTime.isNegative() || maxTime.isZero()) {
            throw new IllegalArgumentException("maxTime debe ser positivo, es " + maxTime);
        }
    }

    public static Budget unlimited() {
        return new Budget(Long.MAX_VALUE, NO_TIME_LIMIT);
    }

    public static Budget ofEvaluations(long maxEvaluations) {
        return new Budget(maxEvaluations, NO_TIME_LIMIT);
    }

    public static Budget ofTime(Duration maxTime) {
        return new Budget(Long.MAX_VALUE, maxTime);
    }

    public Budget withMaxEvaluations(long maxEvaluations) {
        return new Budget(maxEvaluations, maxTime);
    }

    public Budget withMaxTime(Duration maxTime) {
        return new Budget(maxEvaluations, maxTime);
    }

    public boolean limitsEvaluations() {
        return maxEvaluations != Long.MAX_VALUE;
    }

    public boolean limitsTime() {
        return !maxTime.equals(NO_TIME_LIMIT);
    }
}
