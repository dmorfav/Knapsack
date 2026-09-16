package org.dmorfav.knapsack.bench.experiment;

import java.util.function.BiFunction;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;

record FakeSolver(SolverInfo info, BiFunction<Instance, SolverContext, Solution> body) implements KnapsackSolver {

    static FakeSolver of(String family, String variant, BiFunction<Instance, SolverContext, Solution> body) {
        return new FakeSolver(new SolverInfo(family, variant, "Test"), body);
    }

    @Override
    public Solution solve(Instance instance, SolverContext context) {
        return body.apply(instance, context);
    }
}
