package org.dmorfav.knapsack.core.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.Solution;
import org.junit.jupiter.api.Test;

class RunContextTest {

    private final Instance instance = new Instance("pair", 10, List.of(new Item(3, 4), new Item(5, 8)));

    @Test
    void countsEveryEvaluation() {
        RunContext context = new RunContext(instance, Budget.unlimited(), 1);

        context.evaluate(Solution.fromBits(1, 0));
        context.evaluate(Solution.fromBits(1, 0));
        context.evaluate(Solution.fromBits(1, 1));

        assertThat(context.evaluations()).isEqualTo(3);
    }

    @Test
    void allowsExactlyTheEvaluationBudget() {
        RunContext context = new RunContext(instance, Budget.ofEvaluations(2), 1);

        context.evaluate(Solution.empty(2));
        assertThat(context.budgetExhausted()).isFalse();
        context.evaluate(Solution.empty(2));

        assertThat(context.budgetExhausted()).isTrue();
        assertThatThrownBy(() -> context.evaluate(Solution.empty(2)))
                .isInstanceOf(BudgetExhaustedException.class)
                .hasMessageEndingWith("(límite: 2 evaluaciones)");
        assertThat(context.evaluations()).isEqualTo(2);
    }

    @Test
    void exhaustsTheTimeBudget() {
        AtomicLong nanos = new AtomicLong();
        RunContext context = new RunContext(instance, Budget.ofTime(Duration.ofMillis(5)), 1, nanos::get);

        nanos.set(Duration.ofMillis(4).toNanos());
        assertThat(context.budgetExhausted()).isFalse();
        assertThat(context.elapsed()).isEqualTo(Duration.ofMillis(4));

        nanos.set(Duration.ofMillis(5).toNanos());
        assertThat(context.budgetExhausted()).isTrue();
        assertThatThrownBy(() -> context.evaluate(Solution.empty(2)))
                .isInstanceOf(BudgetExhaustedException.class)
                .hasMessageEndingWith("(límite: 5 ms)");
    }

    @Test
    void sameSeedGivesSameRandomSequence() {
        assertThat(firstLongs(42)).isEqualTo(firstLongs(42));
        assertThat(firstLongs(42)).isNotEqualTo(firstLongs(43));
    }

    @Test
    void unlimitedBudgetNeverExhaustsInPractice() {
        Budget unlimited = Budget.unlimited();

        assertThat(unlimited.limitsEvaluations()).isFalse();
        assertThat(unlimited.limitsTime()).isFalse();
        assertThat(new RunContext(instance, unlimited, 1).budgetExhausted()).isFalse();
    }

    private List<Long> firstLongs(long seed) {
        RunContext context = new RunContext(instance, Budget.unlimited(), seed);
        return LongStream.range(0, 5).map(i -> context.random().nextLong()).boxed().toList();
    }
}
