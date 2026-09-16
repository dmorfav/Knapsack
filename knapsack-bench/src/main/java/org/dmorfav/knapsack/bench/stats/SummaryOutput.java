package org.dmorfav.knapsack.bench.stats;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class SummaryOutput {

    private static final List<String> CSV_HEADER = List.of(
            "instance", "solver", "author", "runs", "succeeded", "reference", "reference_kind",
            "best_profit", "mean_profit", "median_profit", "std_profit", "worst_profit",
            "best_gap", "mean_gap", "median_gap", "optimum_hits",
            "median_evaluations", "median_evaluations_to_best", "median_millis");

    private SummaryOutput() {
    }

    public static void writeCsv(List<SummaryRow> rows, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(String.join(",", CSV_HEADER));
        for (SummaryRow row : rows) {
            lines.add(Stream.of(row.instance(), row.solver(), row.author(), row.runs(), row.succeeded(),
                            row.reference(), row.referenceKind(), row.bestProfit(), row.meanProfit(), row.medianProfit(),
                            row.stdProfit(), row.worstProfit(), row.bestGap(), row.meanGap(), row.medianGap(),
                            row.optimumHits(), row.medianEvaluations(), row.medianEvaluationsToBest(), row.medianMillis())
                    .map(SummaryOutput::csvCell)
                    .collect(Collectors.joining(",")));
        }
        Files.write(file, lines);
    }

    /** Tabla legible por instancia; el GAP se marca con ≤ cuando se mide contra la cota y no contra el óptimo. */
    public static void printTable(List<SummaryRow> rows, PrintWriter out) {
        String currentInstance = null;
        for (SummaryRow row : rows) {
            if (!row.instance().equals(currentInstance)) {
                currentInstance = row.instance();
                boolean optimum = "OPTIMUM".equals(row.referenceKind());
                out.printf(Locale.ROOT, "%n%s (%s %d)%n", row.instance(), optimum ? "óptimo" : "cota", row.reference());
                out.printf(Locale.ROOT, "  %-40s %7s %12s %9s %9s %6s %12s %10s%n",
                        "solver", "ok", "mejor", "gap med", "gap mín", "hits", "evals med", "ms med");
            }
            String prefix = "OPTIMUM".equals(row.referenceKind()) ? "" : "≤";
            out.printf(Locale.ROOT, "  %-40s %7s %12s %9s %9s %6s %12s %10s%n",
                    row.solver(),
                    row.succeeded() + "/" + row.runs(),
                    text(row.bestProfit()),
                    row.medianGap() == null ? "-" : prefix + String.format(Locale.ROOT, "%.2f%%", row.medianGap()),
                    row.bestGap() == null ? "-" : prefix + String.format(Locale.ROOT, "%.2f%%", row.bestGap()),
                    text(row.optimumHits()),
                    row.medianEvaluations() == null ? "-" : String.format(Locale.ROOT, "%.0f", row.medianEvaluations()),
                    row.medianMillis() == null ? "-" : String.format(Locale.ROOT, "%.2f", row.medianMillis()));
        }
        out.flush();
    }

    public static void printComparison(Comparison comparison, PrintWriter out) {
        out.printf(Locale.ROOT, "%nComparación global: %d solvers en %d instancias, medida = GAP mediano por instancia%n",
                comparison.solvers().size(), comparison.instances().size());
        if (comparison.friedman() == null) {
            out.printf("  %s%n", comparison.friedmanSkippedReason());
            out.flush();
            return;
        }
        Comparison.Friedman friedman = comparison.friedman();
        out.printf(Locale.ROOT, "  Friedman: χ² = %.3f, p = %.3g%s%n", friedman.statistic(), friedman.pValue(),
                friedman.pValue() < Comparison.ALPHA ? " (hay diferencias significativas)" : " (sin diferencias significativas)");
        if (friedman.criticalDifference() != null) {
            out.printf(Locale.ROOT, "  Diferencia crítica de Nemenyi (α = 0.05): %.3f%n", friedman.criticalDifference());
        }
        out.printf("  %-12s %s%n", "rango medio", "solver");
        Integer[] order = IntStream.range(0, comparison.solvers().size()).boxed()
                .sorted(Comparator.comparingDouble(s -> friedman.averageRanks()[s]))
                .toArray(Integer[]::new);
        for (int s : order) {
            out.printf(Locale.ROOT, "  %-12.2f %s%n", friedman.averageRanks()[s], comparison.solvers().get(s));
        }

        List<Comparison.Pair> significant = comparison.pairs().stream().filter(pair -> pair.better() != null).toList();
        out.printf("%nPares con diferencia significativa (Wilcoxon por instancias, Holm, α = 0.05):%n");
        if (significant.isEmpty()) {
            out.printf(Locale.ROOT, "  ninguno; con %d instancias el test tiene poca potencia%n", comparison.instances().size());
        }
        for (Comparison.Pair pair : significant) {
            String worse = pair.better().equals(pair.first()) ? pair.second() : pair.first();
            out.printf(Locale.ROOT, "  %s mejor que %s (p ajustado %.3g)%n", pair.better(), worse, pair.holmPValue());
        }
        out.flush();
    }

    private static String text(Object value) {
        return value == null ? "-" : value.toString();
    }

    private static String csvCell(Object value) {
        if (value == null) {
            return "";
        }
        String text = value instanceof Double number ? String.format(Locale.ROOT, "%.6f", number) : value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
