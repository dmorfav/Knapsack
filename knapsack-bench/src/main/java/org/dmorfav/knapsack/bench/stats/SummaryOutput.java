package org.dmorfav.knapsack.bench.stats;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
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
