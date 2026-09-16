package org.dmorfav.knapsack.bench.stats;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/** Pruebas no paramétricas para comparar solvers (Demšar, 2006). En todos los datos, menor es mejor. */
public final class HypothesisTests {

    /** q_0.05 de Nemenyi (rango studentizado con infinitos grados de libertad dividido por √2), para k = 2..20. */
    private static final double[] NEMENYI_Q_005 = {
            1.960, 2.343, 2.569, 2.728, 2.850, 2.949, 3.031, 3.102, 3.164,
            3.219, 3.268, 3.313, 3.354, 3.391, 3.426, 3.459, 3.489, 3.517, 3.544};
    private static final int MAX_EXACT_WILCOXON = 50;

    private HypothesisTests() {
    }

    /**
     * Wilcoxon de rangos con signo, bilateral, sobre las diferencias x − y. Descarta las diferencias nulas.
     * Es exacto sin empates y con n ≤ 50; si no, usa la aproximación normal con corrección de empates y de continuidad.
     */
    public static WilcoxonResult wilcoxonSignedRank(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("las muestras emparejadas deben tener el mismo tamaño");
        }
        double[] differences = IntStream.range(0, x.length)
                .mapToDouble(i -> difference(x[i], y[i]))
                .filter(d -> d != 0)
                .toArray();
        int n = differences.length;
        if (n == 0) {
            return new WilcoxonResult(0, 0, 1, true);
        }
        Ranks ranks = Ranks.of(Arrays.stream(differences).map(Math::abs).toArray());
        double positiveRankSum = IntStream.range(0, n).filter(i -> differences[i] > 0).mapToDouble(i -> ranks.ranks[i]).sum();

        if (ranks.tieCorrection == 0 && n <= MAX_EXACT_WILCOXON) {
            return new WilcoxonResult(n, positiveRankSum, exactWilcoxonPValue(n, (int) positiveRankSum), true);
        }
        double mean = n * (n + 1) / 4.0;
        double variance = n * (n + 1) * (2 * n + 1) / 24.0 - ranks.tieCorrection / 48;
        double z = Math.max(0, Math.abs(positiveRankSum - mean) - 0.5) / Math.sqrt(variance);
        return new WilcoxonResult(n, positiveRankSum, Math.min(1, 2 * Distributions.normalSurvival(z)), false);
    }

    /**
     * Friedman con corrección de empates. {@code values[bloque][tratamiento]}; los bloques son instancias y los
     * tratamientos solvers. Incluye la diferencia crítica de Nemenyi con α = 0.05 si k ≤ 20.
     */
    public static FriedmanResult friedman(List<String> treatments, double[][] values) {
        int k = treatments.size();
        int n = values.length;
        if (k < 2 || n < 2) {
            throw new IllegalArgumentException("Friedman necesita al menos 2 tratamientos y 2 bloques");
        }
        double[] rankSums = new double[k];
        double tieCorrection = 0;
        for (double[] block : values) {
            if (block.length != k) {
                throw new IllegalArgumentException("cada bloque necesita un valor por tratamiento");
            }
            Ranks ranks = Ranks.of(block);
            for (int j = 0; j < k; j++) {
                rankSums[j] += ranks.ranks[j];
            }
            tieCorrection += ranks.tieCorrection;
        }
        double expected = n * (k + 1) / 2.0;
        double squares = Arrays.stream(rankSums).map(sum -> (sum - expected) * (sum - expected)).sum();
        double denominator = n * k * (k + 1) - tieCorrection / (k - 1);
        double statistic = denominator == 0 ? 0 : 12 * squares / denominator;
        double[] averageRanks = Arrays.stream(rankSums).map(sum -> sum / n).toArray();
        Double criticalDifference = k - 2 < NEMENYI_Q_005.length
                ? NEMENYI_Q_005[k - 2] * Math.sqrt(k * (k + 1) / (6.0 * n))
                : null;
        return new FriedmanResult(List.copyOf(treatments), averageRanks, n, statistic,
                Distributions.chiSquareSurvival(statistic, k - 1), criticalDifference);
    }

    /** p-valores ajustados por Holm-Bonferroni, en el mismo orden que la entrada. */
    public static double[] holm(double[] pValues) {
        int m = pValues.length;
        Integer[] order = IntStream.range(0, m).boxed().toArray(Integer[]::new);
        Arrays.sort(order, Comparator.comparingDouble(i -> pValues[i]));
        double[] adjusted = new double[m];
        double running = 0;
        for (int rank = 0; rank < m; rank++) {
            running = Math.max(running, Math.min(1, (m - rank) * pValues[order[rank]]));
            adjusted[order[rank]] = running;
        }
        return adjusted;
    }

    /** Resta tolerante a infinitos: dos fallos (∞, ∞) cuentan como empate. */
    private static double difference(double x, double y) {
        return x == y ? 0 : x - y;
    }

    /** Distribución nula exacta de W+ contando subconjuntos de {1..n} por suma. */
    private static double exactWilcoxonPValue(int n, int positiveRankSum) {
        int maxSum = n * (n + 1) / 2;
        double[] counts = new double[maxSum + 1];
        counts[0] = 1;
        for (int rank = 1; rank <= n; rank++) {
            for (int sum = maxSum; sum >= rank; sum--) {
                counts[sum] += counts[sum - rank];
            }
        }
        double total = Math.pow(2, n);
        double lower = 0;
        for (int sum = 0; sum <= positiveRankSum; sum++) {
            lower += counts[sum];
        }
        double upper = 0;
        for (int sum = positiveRankSum; sum <= maxSum; sum++) {
            upper += counts[sum];
        }
        return Math.min(1, 2 * Math.min(lower, upper) / total);
    }

    public record WilcoxonResult(int nonZeroPairs, double positiveRankSum, double pValue, boolean exact) {
    }

    public record FriedmanResult(List<String> treatments, double[] averageRanks, int blocks, double statistic,
                                 double pValue, Double criticalDifference) {
    }
}
