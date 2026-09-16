package org.dmorfav.knapsack.core;

import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

public record Instance(String name, long capacity, List<Item> items, OptionalLong knownOptimum) {

    public Instance {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name no puede estar vacío");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity debe ser >= 0, es " + capacity);
        }
        items = List.copyOf(items);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("la instancia necesita al menos un objeto");
        }
        Objects.requireNonNull(knownOptimum, "knownOptimum");
    }

    public Instance(String name, long capacity, List<Item> items) {
        this(name, capacity, items, OptionalLong.empty());
    }

    public Instance withKnownOptimum(long optimum) {
        return new Instance(name, capacity, items, OptionalLong.of(optimum));
    }

    public int size() {
        return items.size();
    }

    public Item item(int index) {
        return items.get(index);
    }

    public Evaluation evaluate(Solution solution) {
        if (solution.size() != size()) {
            throw new IllegalArgumentException(
                    "la solución tiene " + solution.size() + " posiciones y la instancia " + size());
        }
        long profit = 0;
        long weight = 0;
        for (int i = solution.nextSelected(0); i >= 0; i = solution.nextSelected(i + 1)) {
            Item item = items.get(i);
            profit += item.profit();
            weight += item.weight();
        }
        return new Evaluation(profit, weight, weight <= capacity);
    }
}
