package org.dmorfav.knapsack.bench;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.BiFunction;

import org.dmorfav.knapsack.core.Evaluation;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;
import org.junit.jupiter.api.Test;

class SolverRunTest {

    private final Instance instance = new Instance("trio", 10, List.of(
            new Item(6, 5), new Item(5, 5), new Item(9, 8)));

    @Test
    void recordsAFeasibleSolution() {
        SolverRun run = execute((instance, context) -> {
            context.evaluate(Solution.fromBits(1, 1, 1));
            return Solution.fromBits(1, 1, 0);
        });

        assertThat(run.succeeded()).isTrue();
        assertThat(run.evaluation()).isEqualTo(new Evaluation(11, 10, true));
        assertThat(run.evaluations()).isEqualTo(1);
        assertThat(run.info().id()).isEqualTo("fake/test");
    }

    @Test
    void flagsInfeasibleSolutions() {
        SolverRun run = execute((instance, context) -> Solution.fromBits(1, 1, 1));

        assertThat(run.succeeded()).isFalse();
        assertThat(run.failure()).isEqualTo("solución no factible: peso 18 > capacidad 10");
        assertThat(run.evaluation().feasible()).isFalse();
    }

    @Test
    void flagsProfitAboveTheKnownOptimum() {
        Instance wrongOptimum = instance.withKnownOptimum(10);

        SolverRun run = SolverRun.execute(solver((instance, context) -> Solution.fromBits(1, 1, 0)),
                wrongOptimum, Budget.unlimited(), 1);

        assertThat(run.failure()).startsWith("beneficio 11 supera el óptimo conocido 10");
    }

    @Test
    void flagsNullAndWrongSizedSolutions() {
        assertThat(execute((instance, context) -> null).failure()).contains("null");
        assertThat(execute((instance, context) -> Solution.empty(2)).failure()).contains("2 posiciones");
    }

    @Test
    void reportsBudgetExhaustion() {
        SolverRun run = SolverRun.execute(solver((instance, context) -> {
            while (true) {
                context.evaluate(Solution.empty(instance.size()));
            }
        }), instance, Budget.ofEvaluations(3), 1);

        assertThat(run.succeeded()).isFalse();
        assertThat(run.failure()).contains("presupuesto agotado");
        assertThat(run.evaluations()).isEqualTo(3);
    }

    @Test
    void reportsExceptionsWithoutPropagatingThem() {
        SolverRun run = execute((instance, context) -> {
            throw new IllegalStateException("boom");
        });

        assertThat(run.failure()).isEqualTo("excepción IllegalStateException: boom");
    }

    private SolverRun execute(BiFunction<Instance, SolverContext, Solution> body) {
        return SolverRun.execute(solver(body), instance, Budget.unlimited(), 1);
    }

    private static KnapsackSolver solver(BiFunction<Instance, SolverContext, Solution> body) {
        return new KnapsackSolver() {
            @Override
            public SolverInfo info() {
                return new SolverInfo("fake", "test", "Test");
            }

            @Override
            public Solution solve(Instance instance, SolverContext context) {
                return body.apply(instance, context);
            }
        };
    }
}
