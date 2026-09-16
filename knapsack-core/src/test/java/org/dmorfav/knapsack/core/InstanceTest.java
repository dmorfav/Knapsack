package org.dmorfav.knapsack.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class InstanceTest {

    private final Instance instance = new Instance("tiny", 50, List.of(
            new Item(10, 10),
            new Item(20, 25),
            new Item(30, 30)));

    @Test
    void evaluatesProfitWeightAndFeasibility() {
        assertThat(instance.evaluate(Solution.fromBits(1, 0, 1))).isEqualTo(new Evaluation(40, 40, true));
        assertThat(instance.evaluate(Solution.fromBits(0, 1, 1))).isEqualTo(new Evaluation(50, 55, false));
        assertThat(instance.evaluate(Solution.empty(3))).isEqualTo(new Evaluation(0, 0, true));
    }

    @Test
    void weightEqualToCapacityIsFeasible() {
        Instance exact = new Instance("exact", 35, instance.items());

        assertThat(exact.evaluate(Solution.fromBits(1, 1, 0)).feasible()).isTrue();
    }

    @Test
    void rejectsSolutionsOfAnotherSize() {
        assertThatThrownBy(() -> instance.evaluate(Solution.empty(4)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void itemsAreDefensivelyCopied() {
        List<Item> items = new ArrayList<>(List.of(new Item(1, 1)));
        Instance copy = new Instance("copy", 1, items);

        items.add(new Item(2, 2));

        assertThat(copy.size()).isEqualTo(1);
        assertThatThrownBy(() -> copy.items().add(new Item(3, 3)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsInvalidInstances() {
        List<Item> items = List.of(new Item(1, 1));

        assertThatThrownBy(() -> new Instance(" ", 1, items)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Instance("x", -1, items)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Instance("x", 1, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Item(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Item(1, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
