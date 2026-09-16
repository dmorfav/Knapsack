package org.dmorfav.knapsack.bench.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.dmorfav.knapsack.bench.experiment.RunRecord;
import org.junit.jupiter.api.Test;

class ComparisonTest {

    @Test
    void ranksSolversAndFindsTheSignificantPair() {
        List<RunRecord> records = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String instance = "i" + i;
            for (long seed = 1; seed <= 3; seed++) {
                records.add(StatsTest.ok(instance, "a/best", seed, 100, "OPTIMUM", 100 - seed, 1, 1));
                records.add(StatsTest.ok(instance, "b/middle", seed, 100, "OPTIMUM", 90 - i, 1, 1));
                records.add(StatsTest.ok(instance, "c/worst", seed, 100, "OPTIMUM", 50, 1, 1));
            }
        }

        Comparison comparison = Comparison.of(records);

        assertThat(comparison.solvers()).containsExactly("a/best", "b/middle", "c/worst");
        assertThat(comparison.friedman().averageRanks()).containsExactly(new double[] {1, 2, 3}, within(1e-12));
        assertThat(comparison.friedman().pValue()).isLessThan(0.001);
        assertThat(comparison.friedman().criticalDifference()).isCloseTo(2.343 * Math.sqrt(12 / 60.0), within(1e-12));
        Comparison.Pair ab = comparison.pairs().getFirst();
        assertThat(ab.first()).isEqualTo("a/best");
        assertThat(ab.wins()).isEqualTo(10);
        assertThat(ab.losses()).isZero();
        assertThat(ab.exact()).isTrue();
        assertThat(ab.pValue()).isCloseTo(2.0 / 1024, within(1e-12));
        assertThat(ab.better()).isEqualTo("a/best");
        assertThat(comparison.pairs()).extracting(Comparison.Pair::better).containsOnly("a/best", "b/middle");
    }

    @Test
    void failuresRankLastAndLeaveNoProfitInTheProfile() {
        List<RunRecord> records = List.of(
                StatsTest.ok("i1", "a/ok", 1, 100, "OPTIMUM", 80, 1, 1),
                StatsTest.failed("i1", "b/fails", 1, 100, "OPTIMUM"),
                StatsTest.ok("i2", "a/ok", 1, 100, "OPTIMUM", 50, 1, 1),
                StatsTest.ok("i2", "b/fails", 1, 100, "OPTIMUM", 100, 1, 1));

        Comparison comparison = Comparison.of(records);

        assertThat(comparison.medianGaps()[0][1]).isInfinite();
        assertThat(comparison.friedman().averageRanks()).containsExactly(1.5, 1.5);
        assertThat(comparison.performanceProfile())
                .filteredOn(point -> point.solver().equals("b/fails"))
                .extracting(Comparison.ProfilePoint::fraction)
                .containsOnly(0.5);
        assertThat(comparison.performanceProfile())
                .filteredOn(point -> point.solver().equals("a/ok") && point.tau() == 1.0)
                .extracting(Comparison.ProfilePoint::fraction)
                .containsExactly(0.5);
        assertThat(comparison.performanceProfile())
                .filteredOn(point -> point.solver().equals("a/ok") && point.tau() == 2.0)
                .extracting(Comparison.ProfilePoint::fraction)
                .containsExactly(1.0);
    }

    @Test
    void singleInstanceSkipsFriedmanAndPrintsTheReason() {
        List<RunRecord> records = List.of(
                StatsTest.ok("only", "a/x", 1, 10, "OPTIMUM", 9, 1, 1),
                StatsTest.ok("only", "b/y", 1, 10, "OPTIMUM", 8, 1, 1));
        StringWriter out = new StringWriter();

        Comparison comparison = Comparison.of(records);
        SummaryOutput.printComparison(comparison, new PrintWriter(out));

        assertThat(comparison.friedman()).isNull();
        assertThat(out.toString()).contains("al menos 2 solvers y 2 instancias");
    }

    @Test
    void printsRanksAndSignificantPairs() {
        List<RunRecord> records = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            records.add(StatsTest.ok("i" + i, "a/best", 1, 100, "OPTIMUM", 99, 1, 1));
            records.add(StatsTest.ok("i" + i, "z/worst", 1, 100, "OPTIMUM", 10 + i, 1, 1));
        }
        StringWriter out = new StringWriter();

        SummaryOutput.printComparison(Comparison.of(records), new PrintWriter(out));

        assertThat(out.toString())
                .contains("2 solvers en 10 instancias", "hay diferencias significativas", "1.00         a/best")
                .contains("a/best mejor que z/worst (p ajustado 0.00195)");
    }
}
