package org.dmorfav.knapsack.bench.stats;

import java.util.Arrays;

public final class Descriptive {

    private Descriptive() {
    }

    public static Double mean(double[] values) {
        return values.length == 0 ? null : Arrays.stream(values).sum() / values.length;
    }

    public static Double median(double[] values) {
        if (values.length == 0) {
            return null;
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
    }

    /** Desviación típica muestral (n − 1); null con menos de dos valores. */
    public static Double sampleStandardDeviation(double[] values) {
        if (values.length < 2) {
            return null;
        }
        double mean = mean(values);
        double squares = Arrays.stream(values).map(value -> (value - mean) * (value - mean)).sum();
        return Math.sqrt(squares / (values.length - 1));
    }
}
