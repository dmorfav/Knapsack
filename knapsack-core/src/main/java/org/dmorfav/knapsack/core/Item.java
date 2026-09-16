package org.dmorfav.knapsack.core;

public record Item(long profit, long weight) {

    public Item {
        if (profit < 0) {
            throw new IllegalArgumentException("profit debe ser >= 0, es " + profit);
        }
        if (weight < 0) {
            throw new IllegalArgumentException("weight debe ser >= 0, es " + weight);
        }
    }
}
