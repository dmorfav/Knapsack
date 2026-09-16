package org.dmorfav.knapsack.bench.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.dmorfav.knapsack.bench.experiment.RunRecord;

/**
 * Comparación estadística entre solvers usando cada instancia como bloque y el GAP mediano de las ejecuciones
 * correctas como medida (menor es mejor). Un solver sin ejecuciones correctas en una instancia cuenta como GAP
 * infinito, así que queda último en ella.
 */
public record Comparison(List<String> solvers, List<String> instances, double[][] medianGaps, Friedman friedman,
                         String friedmanSkippedReason, List<Pair> pairs, List<ProfilePoint> performanceProfile) {

    public static final double ALPHA = 0.05;

    public record Friedman(double statistic, double pValue, double[] averageRanks, Double criticalDifference) {
    }

    /**
     * Wilcoxon sobre los GAP medianos por instancia. {@code better} es el solver con menor GAP cuando el p-valor
     * ajustado por Holm es menor que {@link #ALPHA}; si no, null.
     */
    public record Pair(String first, String second, int wins, int losses, int ties, double pValue, double holmPValue,
                       boolean exact, String better) {
    }

    /** Fracción de instancias en las que el beneficio mediano del solver está a un factor ≤ tau del mejor. */
    public record ProfilePoint(String solver, double tau, double fraction) {
    }

    public static Comparison of(List<RunRecord> records) {
        List<String> solvers = List.copyOf(new TreeSet<>(records.stream().map(RunRecord::solver).toList()));
        List<String> instances = List.copyOf(new TreeSet<>(records.stream().map(RunRecord::instance).toList()));
        double[][] gaps = new double[instances.size()][solvers.size()];
        double[][] profits = new double[instances.size()][solvers.size()];
        for (int i = 0; i < instances.size(); i++) {
            for (int s = 0; s < solvers.size(); s++) {
                String instance = instances.get(i);
                String solver = solvers.get(s);
                List<RunRecord> ok = records.stream()
                        .filter(r -> r.instance().equals(instance) && r.solver().equals(solver) && r.succeeded())
                        .toList();
                Double gap = Descriptive.median(ok.stream().mapToDouble(RunRecord::gap).toArray());
                Double profit = Descriptive.median(ok.stream().mapToDouble(RunRecord::profit).toArray());
                gaps[i][s] = gap == null ? Double.POSITIVE_INFINITY : gap;
                profits[i][s] = profit == null ? 0 : profit;
            }
        }

        Friedman friedman = null;
        String skipped = null;
        if (solvers.size() < 2 || instances.size() < 2) {
            skipped = "Friedman necesita al menos 2 solvers y 2 instancias";
        } else {
            HypothesisTests.FriedmanResult result = HypothesisTests.friedman(solvers, gaps);
            friedman = new Friedman(result.statistic(), result.pValue(), result.averageRanks(), result.criticalDifference());
        }

        return new Comparison(solvers, instances, gaps, friedman, skipped, pairs(solvers, gaps),
                performanceProfile(solvers, profits));
    }

    private static List<Pair> pairs(List<String> solvers, double[][] gaps) {
        List<int[]> indexes = new ArrayList<>();
        List<HypothesisTests.WilcoxonResult> results = new ArrayList<>();
        for (int a = 0; a < solvers.size(); a++) {
            for (int b = a + 1; b < solvers.size(); b++) {
                indexes.add(new int[] {a, b});
                results.add(HypothesisTests.wilcoxonSignedRank(column(gaps, a), column(gaps, b)));
            }
        }
        double[] holm = HypothesisTests.holm(results.stream().mapToDouble(HypothesisTests.WilcoxonResult::pValue).toArray());
        List<Pair> pairs = new ArrayList<>();
        for (int p = 0; p < indexes.size(); p++) {
            int a = indexes.get(p)[0];
            int b = indexes.get(p)[1];
            int wins = 0;
            int losses = 0;
            int ties = 0;
            for (double[] block : gaps) {
                int comparison = Double.compare(block[a], block[b]);
                if (comparison < 0) {
                    wins++;
                } else if (comparison > 0) {
                    losses++;
                } else {
                    ties++;
                }
            }
            HypothesisTests.WilcoxonResult result = results.get(p);
            double expectedRankSum = result.nonZeroPairs() * (result.nonZeroPairs() + 1) / 4.0;
            String better = holm[p] >= ALPHA ? null
                    : result.positiveRankSum() > expectedRankSum ? solvers.get(b) : solvers.get(a);
            pairs.add(new Pair(solvers.get(a), solvers.get(b), wins, losses, ties, result.pValue(), holm[p],
                    result.exact(), better));
        }
        return pairs;
    }

    /** Perfiles de Dolan-Moré para maximización: ratio = mejor beneficio mediano / beneficio mediano del solver. */
    private static List<ProfilePoint> performanceProfile(List<String> solvers, double[][] profits) {
        int instances = profits.length;
        double[][] ratios = new double[instances][solvers.size()];
        TreeSet<Double> taus = new TreeSet<>();
        for (int i = 0; i < instances; i++) {
            double best = 0;
            for (double profit : profits[i]) {
                best = Math.max(best, profit);
            }
            for (int s = 0; s < solvers.size(); s++) {
                ratios[i][s] = best == 0 ? 1 : profits[i][s] == 0 ? Double.POSITIVE_INFINITY : best / profits[i][s];
                if (Double.isFinite(ratios[i][s])) {
                    taus.add(ratios[i][s]);
                }
            }
        }
        Map<String, List<ProfilePoint>> bySolver = new TreeMap<>();
        for (int s = 0; s < solvers.size(); s++) {
            for (double tau : taus) {
                int covered = 0;
                for (double[] ratio : ratios) {
                    if (ratio[s] <= tau) {
                        covered++;
                    }
                }
                bySolver.computeIfAbsent(solvers.get(s), key -> new ArrayList<>())
                        .add(new ProfilePoint(solvers.get(s), tau, (double) covered / instances));
            }
        }
        return bySolver.values().stream().flatMap(List::stream).toList();
    }

    private static double[] column(double[][] matrix, int column) {
        double[] values = new double[matrix.length];
        for (int row = 0; row < matrix.length; row++) {
            values[row] = matrix[row][column];
        }
        return values;
    }
}
