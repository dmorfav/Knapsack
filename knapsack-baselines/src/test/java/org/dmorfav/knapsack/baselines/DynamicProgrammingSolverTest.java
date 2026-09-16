package org.dmorfav.knapsack.baselines;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import org.dmorfav.knapsack.core.Evaluation;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.generator.GeneratorSpec;
import org.dmorfav.knapsack.core.generator.InstanceClass;
import org.dmorfav.knapsack.core.generator.PisingerGenerator;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.BudgetExhaustedException;
import org.dmorfav.knapsack.core.solver.RunContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class DynamicProgrammingSolverTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("smallInstances")
    void matchesBruteForceOnSmallInstances(Instance instance) {
        Evaluation evaluation = instance.evaluate(DynamicProgrammingSolver.optimum(instance));

        assertThat(evaluation.feasible()).isTrue();
        assertThat(evaluation.profit()).isEqualTo(BruteForce.optimumProfit(instance));
    }

    @Test
    void handlesZeroWeightItemsAndItemsHeavierThanCapacity() {
        Instance instance = new Instance("edge", 5, List.of(
                new Item(4, 0), new Item(100, 6), new Item(3, 5), new Item(0, 0)));

        Solution optimum = DynamicProgrammingSolver.optimum(instance);

        assertThat(instance.evaluate(optimum)).isEqualTo(new Evaluation(7, 5, true));
        assertThat(optimum.isSelected(1)).isFalse();
    }

    @Test
    void solverEvaluatesOnlyTheOptimum() {
        Instance instance = PisingerGenerator.generate(new GeneratorSpec(InstanceClass.STRONGLY_CORRELATED, 30, 100, 50, 100, 5));
        RunContext context = new RunContext(instance, Budget.unlimited(), 1);

        Solution solution = new DynamicProgrammingSolver().solve(instance, context);

        assertThat(context.evaluations()).isEqualTo(1);
        assertThat(solution).isEqualTo(DynamicProgrammingSolver.optimum(instance));
    }

    @Test
    void stopsWhenTheBudgetIsExhausted() {
        Instance instance = PisingerGenerator.generate(new GeneratorSpec(InstanceClass.UNCORRELATED, 50, 100, 50, 100, 5));
        RunContext context = new RunContext(instance, Budget.ofTime(Duration.ofNanos(1)), 1);

        assertThatThrownBy(() -> new DynamicProgrammingSolver().solve(instance, context))
                .isInstanceOf(BudgetExhaustedException.class);
    }

    @Test
    void refusesInstancesBeyondTheCellLimit() {
        Instance huge = new Instance("huge", DynamicProgrammingSolver.MAX_CELLS, List.of(new Item(1, 1)));
        Instance wide = new Instance("wide", 999_999, Stream.generate(() -> new Item(1, 1)).limit(2_000).toList());

        assertThat(DynamicProgrammingSolver.isTractable(huge)).isFalse();
        assertThat(DynamicProgrammingSolver.isTractable(wide)).isFalse();
        assertThatThrownBy(() -> DynamicProgrammingSolver.optimum(wide))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("límite");
    }

    static Stream<Instance> smallInstances() {
        return Stream.of(InstanceClass.values()).flatMap(type -> Stream.of(1L, 2L, 3L).flatMap(seed -> Stream.of(
                PisingerGenerator.generate(new GeneratorSpec(type, 14, 100, 30, 100, seed)),
                PisingerGenerator.generate(new GeneratorSpec(type, 14, 100, 70, 100, seed)))));
    }
}
