package org.dmorfav.knapsack.baselines;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.BudgetExhaustedException;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;

/**
 * Óptimo exacto por programación dinámica sobre la capacidad: O(n·C) en tiempo y n·(C+1) bits de decisiones.
 * Solo es viable si n·(C+1) no supera {@link #MAX_CELLS}.
 */
public final class DynamicProgrammingSolver implements KnapsackSolver {

    public static final long MAX_CELLS = 1_000_000_000L;

    private static final SolverInfo INFO = new SolverInfo("exact", "dynamic-programming", "baseline");

    @Override
    public SolverInfo info() {
        return INFO;
    }

    @Override
    public Solution solve(Instance instance, SolverContext context) {
        Solution optimum = solve(instance, () -> {
            if (context.budgetExhausted()) {
                throw new BudgetExhaustedException(context.budget(), context.evaluations(), context.elapsed());
            }
        });
        context.evaluate(optimum);
        return optimum;
    }

    public static boolean isTractable(Instance instance) {
        return cells(instance) <= MAX_CELLS;
    }

    public static Solution optimum(Instance instance) {
        return solve(instance, () -> { });
    }

    private static long cells(Instance instance) {
        if (instance.capacity() >= MAX_CELLS) {
            return Long.MAX_VALUE;
        }
        return instance.size() * (instance.capacity() + 1);
    }

    /** {@code checkpoint} se invoca antes de cada objeto y puede interrumpir el cálculo lanzando una excepción. */
    private static Solution solve(Instance instance, Runnable checkpoint) {
        long cells = cells(instance);
        if (cells > MAX_CELLS) {
            throw new IllegalStateException("la programación dinámica necesita n·(C+1) = " + instance.size() + "·("
                    + instance.capacity() + "+1) celdas y el límite es " + MAX_CELLS);
        }
        int n = instance.size();
        int capacity = (int) instance.capacity();
        int width = capacity + 1;
        long[] best = new long[width];
        long[] taken = new long[(int) ((cells + Long.SIZE - 1) / Long.SIZE)];

        for (int i = 0; i < n; i++) {
            checkpoint.run();
            Item item = instance.item(i);
            if (item.weight() > capacity) {
                continue;
            }
            int weight = (int) item.weight();
            long row = (long) i * width;
            for (int c = capacity; c >= weight; c--) {
                long candidate = best[c - weight] + item.profit();
                if (candidate > best[c]) {
                    best[c] = candidate;
                    long bit = row + c;
                    taken[(int) (bit >>> 6)] |= 1L << bit;
                }
            }
        }

        boolean[] selected = new boolean[n];
        int c = capacity;
        for (int i = n - 1; i >= 0; i--) {
            long bit = (long) i * width + c;
            if ((taken[(int) (bit >>> 6)] & (1L << bit)) != 0) {
                selected[i] = true;
                c -= (int) instance.item(i).weight();
            }
        }
        return Solution.of(selected);
    }
}
