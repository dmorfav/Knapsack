package org.dmorfav.knapsack.core.solver;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.dmorfav.knapsack.core.Evaluation;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;

/** Contexto de una única ejecución. No es thread-safe: un solver lo usa desde un solo hilo. */
public final class RunContext implements SolverContext {

    private static final String RANDOM_ALGORITHM = "L64X128MixRandom";

    private final Instance instance;
    private final Budget budget;
    private final RandomGenerator random;
    private final LongSupplier nanoClock;
    private final long startNanos;
    private final List<TracePoint> trace = new ArrayList<>();
    private long evaluations;
    private long bestFeasibleProfit = -1;

    public RunContext(Instance instance, Budget budget, long seed) {
        this(instance, budget, seed, System::nanoTime);
    }

    RunContext(Instance instance, Budget budget, long seed, LongSupplier nanoClock) {
        this.instance = Objects.requireNonNull(instance, "instance");
        this.budget = Objects.requireNonNull(budget, "budget");
        this.random = RandomGeneratorFactory.of(RANDOM_ALGORITHM).create(seed);
        this.nanoClock = nanoClock;
        this.startNanos = nanoClock.getAsLong();
    }

    @Override
    public RandomGenerator random() {
        return random;
    }

    @Override
    public Evaluation evaluate(Solution solution) {
        if (budgetExhausted()) {
            throw new BudgetExhaustedException(budget, evaluations, elapsed());
        }
        evaluations++;
        Evaluation evaluation = instance.evaluate(solution);
        if (evaluation.feasible() && evaluation.profit() > bestFeasibleProfit) {
            bestFeasibleProfit = evaluation.profit();
            trace.add(new TracePoint(evaluations, nanoClock.getAsLong() - startNanos, evaluation.profit()));
        }
        return evaluation;
    }

    /** Curva de convergencia: cada vez que una evaluación mejoró el mejor beneficio factible visto. */
    public List<TracePoint> trace() {
        return List.copyOf(trace);
    }

    @Override
    public long evaluations() {
        return evaluations;
    }

    @Override
    public Duration elapsed() {
        return Duration.ofNanos(nanoClock.getAsLong() - startNanos);
    }

    @Override
    public Budget budget() {
        return budget;
    }

    @Override
    public boolean budgetExhausted() {
        return evaluations >= budget.maxEvaluations() || elapsed().compareTo(budget.maxTime()) >= 0;
    }
}
