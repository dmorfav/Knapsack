package org.dmorfav.knapsack.bench.legacy;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.IntStream;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.RunContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LegacySolversTest {

    private static final Instance LOOSE = new Instance("loose", 1_000,
            IntStream.rangeClosed(1, 8).mapToObj(i -> new Item(i * 3L, i * 2L)).toList());

    @Test
    void serviceLoaderDiscoversEveryLegacyAlgorithm() {
        assertThat(ServiceLoader.load(KnapsackSolver.class).stream().map(ServiceLoader.Provider::get))
                .extracting(solver -> solver.info().id())
                .contains(
                        "discrete-binary-harmony-search/legacy",
                        "harmony-search/legacy",
                        "hill-climbing-best-ascent/legacy",
                        "hill-climbing-first-ascent/legacy",
                        "improved-harmony-search/legacy");
    }

    @ParameterizedTest
    @MethodSource("legacySolvers")
    void runsThroughTheContractAndCountsEvaluations(KnapsackSolver solver) {
        RunContext context = new RunContext(LOOSE, Budget.ofTime(Duration.ofSeconds(10)), 1);

        Solution solution = solver.solve(LOOSE, context);

        assertThat(solution.size()).isEqualTo(LOOSE.size());
        assertThat(context.evaluations()).isPositive();
        assertThat(solver.info().author()).isEqualTo(solver.info().family().equals("hill-climbing-best-ascent")
                ? "Daniel Morfa Vega" : "Jorge Herrera Franklin");
    }

    @Test
    void translatesAssignmentsWithTheLegacySemantics() {
        int[] assignment = {1, 0, 2, -1, 1, 1};

        assertThat(CountingKnapsack.toSolution(assignment, 5)).hasToString("10001");
    }

    @Test
    void everyLegacyModelQueryCountsAsOneEvaluation() {
        RunContext context = new RunContext(LOOSE, Budget.unlimited(), 1);
        CountingKnapsack knapsack = new CountingKnapsack(LOOSE, context);
        int[] firstTwo = {1, 1, 0, 0, 0, 0, 0, 0};

        assertThat(knapsack.evaluateOF(firstTwo)).isEqualTo(9f);
        assertThat(knapsack.totalWeight(firstTwo)).isEqualTo(6f);
        assertThat(knapsack.evaluateRestriction(firstTwo)).isTrue();
        assertThat(knapsack.instanceSize()).isEqualTo(8);
        assertThat(context.evaluations()).isEqualTo(3);
    }

    static List<KnapsackSolver> legacySolvers() {
        return List.of(
                new LegacySolvers.LegacyHarmonySearch(),
                new LegacySolvers.LegacyImprovedHarmonySearch(),
                new LegacySolvers.LegacyDiscreteBinaryHarmonySearch(),
                new LegacySolvers.LegacyHillClimbingFirstAscent(),
                new LegacySolvers.LegacyHillClimbingBestAscent());
    }
}
