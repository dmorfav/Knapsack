package org.dmorfav.knapsack.baselines;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.RunContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class GreedySolversTest {

    /** Caso clásico: el voraz se queda con el objeto de mejor ratio y pierde el grande. */
    private final Instance trap = new Instance("trap", 100, List.of(new Item(2, 1), new Item(100, 100)));

    @Test
    void greedyTakesItemsByDecreasingRatioWhileTheyFit() {
        Instance instance = new Instance("ratios", 10, List.of(
                new Item(6, 6), new Item(5, 2), new Item(8, 4), new Item(1, 3)));

        assertThat(solve(new GreedySolver(), instance)).hasToString("0111");
    }

    @Test
    void greedyKeepsFillingAfterAnItemDoesNotFit() {
        Instance instance = new Instance("skip", 5, List.of(new Item(10, 4), new Item(9, 4), new Item(1, 1)));

        assertThat(solve(new GreedySolver(), instance)).hasToString("101");
    }

    @Test
    void greedyFallsIntoTheTrapAndBestItemEscapesIt() {
        assertThat(trap.evaluate(solve(new GreedySolver(), trap)).profit()).isEqualTo(2);
        assertThat(trap.evaluate(solve(new GreedyBestItemSolver(), trap)).profit()).isEqualTo(100);
    }

    @Test
    void bestItemIgnoresItemsThatDoNotFitAlone() {
        Instance instance = new Instance("heavy", 3, List.of(new Item(50, 4), new Item(2, 3)));

        assertThat(GreedyBestItemSolver.bestSingleItem(instance)).hasToString("01");
        assertThat(GreedyBestItemSolver.bestSingleItem(new Instance("none", 1, List.of(new Item(5, 2)))))
                .hasToString("0");
    }

    @Test
    void ratiosAreComparedExactly() {
        Item third = new Item(1, 3);
        Item almostThird = new Item(333_333_333_333L, 1_000_000_000_000L);
        Item huge = new Item(Long.MAX_VALUE / 2, Long.MAX_VALUE / 3);

        assertThat(RatioOrder.compareRatios(third, almostThird)).isPositive();
        assertThat(RatioOrder.compareRatios(new Item(2, 6), third)).isZero();
        assertThat(RatioOrder.compareRatios(new Item(1, 0), huge)).isPositive();
        assertThat(RatioOrder.compareRatios(huge, new Item(1, 1))).isPositive();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generatedInstances")
    void bestItemGuaranteesHalfTheOptimum(Instance instance) {
        long optimum = BruteForce.optimumProfit(instance);
        Evaluation greedy = instance.evaluate(solve(new GreedySolver(), instance));
        Evaluation bestItem = instance.evaluate(solve(new GreedyBestItemSolver(), instance));

        assertThat(greedy.feasible()).isTrue();
        assertThat(bestItem.feasible()).isTrue();
        assertThat(bestItem.profit()).isGreaterThanOrEqualTo(greedy.profit());
        assertThat(2 * bestItem.profit()).isGreaterThanOrEqualTo(optimum);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("generatedInstances")
    void dantzigBoundsTheOptimumFromAbove(Instance instance) {
        long optimum = BruteForce.optimumProfit(instance);
        long greedy = instance.evaluate(solve(new GreedySolver(), instance)).profit();

        long maxProfit = instance.items().stream().mapToLong(Item::profit).max().orElseThrow();

        assertThat(UpperBounds.dantzig(instance)).isGreaterThanOrEqualTo(optimum);
        assertThat(UpperBounds.dantzig(instance)).isLessThanOrEqualTo(greedy + maxProfit);
    }

    @Test
    void dantzigAddsTheFractionOfTheCriticalItem() {
        Instance instance = new Instance("fraction", 10, List.of(new Item(12, 6), new Item(10, 8), new Item(1, 1)));

        assertThat(UpperBounds.dantzig(instance)).isEqualTo(12 + 10 * 4 / 8);
    }

    static Stream<Instance> generatedInstances() {
        return Stream.of(InstanceClass.values()).flatMap(type -> Stream.of(11L, 12L).map(seed ->
                PisingerGenerator.generate(new GeneratorSpec(type, 12, 100, 40, 100, seed))));
    }

    private static Solution solve(KnapsackSolver solver, Instance instance) {
        return solver.solve(instance, new RunContext(instance, Budget.unlimited(), 1));
    }
}
