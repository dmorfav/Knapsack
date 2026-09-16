package org.dmorfav.knapsack.bench.stats;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

/** Rangos 1..n con empates promediados, y la suma Σ(t³ − t) de los grupos de empate. */
final class Ranks {

    final double[] ranks;
    final double tieCorrection;

    private Ranks(double[] ranks, double tieCorrection) {
        this.ranks = ranks;
        this.tieCorrection = tieCorrection;
    }

    static Ranks of(double[] values) {
        Integer[] order = IntStream.range(0, values.length).boxed().toArray(Integer[]::new);
        Arrays.sort(order, Comparator.comparingDouble(i -> values[i]));
        double[] ranks = new double[values.length];
        double tieCorrection = 0;
        int i = 0;
        while (i < order.length) {
            int j = i;
            while (j + 1 < order.length && Double.compare(values[order[j + 1]], values[order[i]]) == 0) {
                j++;
            }
            double averageRank = (i + j) / 2.0 + 1;
            for (int k = i; k <= j; k++) {
                ranks[order[k]] = averageRank;
            }
            int t = j - i + 1;
            tieCorrection += (double) t * t * t - t;
            i = j + 1;
        }
        return new Ranks(ranks, tieCorrection);
    }
}
