package org.dmorfav.knapsack.bench.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.dmorfav.knapsack.bench.experiment.RunRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StatsTest {

    @TempDir
    Path tempDir;

    @Test
    void descriptiveStatistics() {
        double[] values = {4, 1, 3, 2};

        assertThat(Descriptive.mean(values)).isEqualTo(2.5);
        assertThat(Descriptive.median(values)).isEqualTo(2.5);
        assertThat(Descriptive.median(new double[] {5, 1, 3})).isEqualTo(3);
        assertThat(Descriptive.sampleStandardDeviation(values)).isCloseTo(1.2909944, within(1e-6));
        assertThat(Descriptive.mean(new double[0])).isNull();
        assertThat(Descriptive.median(new double[0])).isNull();
        assertThat(Descriptive.sampleStandardDeviation(new double[] {1})).isNull();
        assertThat(values).containsExactly(4, 1, 3, 2);
    }

    @Test
    void summarizesSuccessfulRunsAndCountsFailures() {
        List<RunRecord> records = List.of(
                ok("i1", "b/solver", 1, 100, "OPTIMUM", 100, 10, 4),
                ok("i1", "b/solver", 2, 100, "OPTIMUM", 90, 30, null),
                failed("i1", "b/solver", 3, 100, "OPTIMUM"),
                ok("i1", "a/solver", 1, 100, "OPTIMUM", 95, 20, 2),
                ok("i2", "a/solver", 1, 200, "UPPER_BOUND", 150, 5, 5));

        List<SummaryRow> rows = Summarizer.summarize(records);

        assertThat(rows).extracting(row -> row.instance() + " " + row.solver())
                .containsExactly("i1 a/solver", "i1 b/solver", "i2 a/solver");
        SummaryRow b = rows.get(1);
        assertThat(b.runs()).isEqualTo(3);
        assertThat(b.succeeded()).isEqualTo(2);
        assertThat(b.bestProfit()).isEqualTo(100);
        assertThat(b.worstProfit()).isEqualTo(90);
        assertThat(b.meanProfit()).isEqualTo(95);
        assertThat(b.bestGap()).isEqualTo(0);
        assertThat(b.medianGap()).isEqualTo(5);
        assertThat(b.optimumHits()).isEqualTo(1);
        assertThat(b.medianEvaluations()).isEqualTo(20);
        assertThat(b.medianEvaluationsToBest()).isEqualTo(4);
        assertThat(rows.get(2).optimumHits()).isNull();
        assertThat(rows.get(2).stdProfit()).isNull();
    }

    @Test
    void groupWithoutSuccessesHasNoQualityMetrics() {
        SummaryRow row = Summarizer.summarize(List.of(failed("i", "x/y", 1, 10, "OPTIMUM"))).getFirst();

        assertThat(row.succeeded()).isZero();
        assertThat(row.bestProfit()).isNull();
        assertThat(row.medianGap()).isNull();
        assertThat(row.optimumHits()).isZero();
    }

    @Test
    void writesCsvAndPrintsTable() throws IOException {
        List<SummaryRow> rows = Summarizer.summarize(List.of(
                ok("inst", "greedy/ratio", 1, 100, "OPTIMUM", 90, 1, 1),
                ok("bounded", "greedy/ratio", 1, 100, "UPPER_BOUND", 80, 1, 1)));
        Path csv = tempDir.resolve("summary.csv");
        StringWriter table = new StringWriter();

        SummaryOutput.writeCsv(rows, csv);
        SummaryOutput.printTable(rows, new PrintWriter(table));

        List<String> lines = Files.readAllLines(csv);
        assertThat(lines).hasSize(3);
        assertThat(lines.getFirst()).startsWith("instance,solver,author,runs,succeeded");
        assertThat(lines.get(2)).startsWith("inst,greedy/ratio,Test,1,1,100,OPTIMUM,90,90.000000");
        assertThat(table.toString()).contains("inst (óptimo 100)", "10.00%", "bounded (cota 100)", "≤20.00%");
    }

    static RunRecord ok(String instance, String solver, long seed, long reference, String kind, long profit,
                        long evaluations, Integer toBest) {
        return new RunRecord(instance, instance + ".json", solver, solver.split("/")[0], solver.split("/")[1], "Test",
                seed, RunRecord.OK, null, profit, profit, 1, evaluations, evaluations * 1_000_000L, reference, kind,
                100.0 * (reference - profit) / reference, toBest == null ? null : toBest.longValue(), null, "1");
    }

    static RunRecord failed(String instance, String solver, long seed, long reference, String kind) {
        return new RunRecord(instance, instance + ".json", solver, solver.split("/")[0], solver.split("/")[1], "Test",
                seed, RunRecord.FAILED, "boom", null, null, null, 7, 1, reference, kind, null, null, null, null);
    }
}
