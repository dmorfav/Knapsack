package org.dmorfav.knapsack.bench.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Valores de referencia calculados con scipy 1.17.1 (scipy.stats.wilcoxon, friedmanchisquare, chi2.sf, norm.sf). */
class HypothesisTestsTest {

    @Test
    void logGammaMatchesKnownValues() {
        assertThat(Distributions.logGamma(0.5)).isCloseTo(Math.log(Math.sqrt(Math.PI)), within(1e-13));
        assertThat(Distributions.logGamma(1.5)).isCloseTo(Math.log(Math.sqrt(Math.PI) / 2), within(1e-13));
        assertThat(Distributions.logGamma(1)).isCloseTo(0, within(1e-13));
        assertThat(Distributions.logGamma(5)).isCloseTo(Math.log(24), within(1e-13));
        assertThat(Distributions.logGamma(30)).isCloseTo(71.25703896716800, within(1e-11));
    }

    @Test
    void distributionTailsMatchScipy() {
        assertThat(Distributions.chiSquareSurvival(7.5, 3)).isCloseTo(0.057558451972636406, within(1e-10));
        assertThat(Distributions.chiSquareSurvival(0.3, 1)).isCloseTo(0.583882420770365, within(1e-10));
        assertThat(Distributions.chiSquareSurvival(40, 10)).isCloseTo(1.694474393006737e-05, within(1e-12));
        assertThat(Distributions.normalSurvival(1.96)).isCloseTo(0.024997895148220435, within(1e-10));
        assertThat(Distributions.normalSurvival(0.3)).isCloseTo(0.3820885778110474, within(1e-10));
        assertThat(Distributions.normalSurvival(3.5)).isCloseTo(0.00023262907903552502, within(1e-12));
        assertThat(Distributions.normalSurvival(-1.96)).isCloseTo(1 - 0.024997895148220435, within(1e-10));
    }

    @Test
    void exactWilcoxonMatchesScipy() {
        double[] x = {1.83, 0.50, 1.62, 2.48, 1.68, 1.88, 1.55, 3.06, 1.30};
        double[] y = {0.878, 0.647, 0.598, 2.05, 1.06, 1.29, 1.06, 3.14, 1.29};

        HypothesisTests.WilcoxonResult result = HypothesisTests.wilcoxonSignedRank(x, y);

        assertThat(result.exact()).isTrue();
        assertThat(result.positiveRankSum()).isEqualTo(40);
        assertThat(result.pValue()).isCloseTo(0.0390625, within(1e-12));
        assertThat(HypothesisTests.wilcoxonSignedRank(new double[] {1, 2, 3, 4, 5, 6, 7, 8}, new double[8]).pValue())
                .isCloseTo(0.0078125, within(1e-12));
    }

    @Test
    void approximateWilcoxonWithTiesAndZerosMatchesScipy() {
        double[] a = {3, 5, 5, 8, 2, 7, 7, 9, 4, 6, 1, 10, 2, 2};
        double[] b = {1, 5, 3, 4, 2, 3, 9, 2, 2, 1, 4, 3, 1, 6};

        HypothesisTests.WilcoxonResult result = HypothesisTests.wilcoxonSignedRank(a, b);

        assertThat(result.exact()).isFalse();
        assertThat(result.nonZeroPairs()).isEqualTo(12);
        assertThat(result.positiveRankSum()).isEqualTo(60.5);
        assertThat(result.pValue()).isCloseTo(0.09752702243768838, within(1e-10));
    }

    @Test
    void wilcoxonTreatsBothFailuresAsATieAndIdenticalSamplesAsNoEvidence() {
        double inf = Double.POSITIVE_INFINITY;

        HypothesisTests.WilcoxonResult result = HypothesisTests.wilcoxonSignedRank(
                new double[] {inf, 1, 2}, new double[] {inf, 1, 2});

        assertThat(result.nonZeroPairs()).isZero();
        assertThat(result.pValue()).isEqualTo(1);
        assertThatThrownBy(() -> HypothesisTests.wilcoxonSignedRank(new double[1], new double[2]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void friedmanWithTiesMatchesScipy() {
        double[][] roundingTimes = {
                {5.40, 5.50, 5.55}, {5.85, 5.70, 5.75}, {5.20, 5.60, 5.50}, {5.55, 5.50, 5.40}, {5.90, 5.85, 5.70},
                {5.45, 5.55, 5.60}, {5.40, 5.40, 5.35}, {5.45, 5.50, 5.35}, {5.25, 5.15, 5.00}, {5.85, 5.80, 5.70},
                {5.25, 5.20, 5.10}, {5.65, 5.55, 5.45}, {5.60, 5.35, 5.45}, {5.05, 5.00, 4.95}, {5.50, 5.50, 5.40},
                {5.45, 5.55, 5.50}, {5.55, 5.55, 5.35}, {5.45, 5.50, 5.55}, {5.50, 5.45, 5.25}, {5.65, 5.60, 5.40},
                {5.70, 5.65, 5.55}, {6.30, 6.30, 6.25}};

        HypothesisTests.FriedmanResult result = HypothesisTests.friedman(List.of("a", "b", "c"), roundingTimes);

        assertThat(result.statistic()).isCloseTo(11.142857142857132, within(1e-9));
        assertThat(result.pValue()).isCloseTo(0.003805040775511383, within(1e-10));
        assertThat(result.averageRanks()).containsExactly(
                new double[] {2.409090909090909, 2.1363636363636362, 1.4545454545454546}, within(1e-12));
        assertThat(result.criticalDifference()).isCloseTo(2.343 * Math.sqrt(3 * 4 / (6.0 * 22)), within(1e-12));
    }

    @Test
    void friedmanWithoutTiesMatchesScipy() {
        double[][] values = {
                {0.1, 0.3, 0.2, 0.9}, {0.4, 0.5, 0.1, 0.8}, {0.2, 0.6, 0.3, 0.7}, {0.05, 0.2, 0.4, 0.95},
                {0.3, 0.35, 0.25, 0.6}};

        HypothesisTests.FriedmanResult result = HypothesisTests.friedman(List.of("a", "b", "c", "d"), values);

        assertThat(result.statistic()).isCloseTo(12.12, within(1e-9));
        assertThat(result.pValue()).isCloseTo(0.00698319445326557, within(1e-10));
    }

    @Test
    void friedmanRanksFailuresLast() {
        double inf = Double.POSITIVE_INFINITY;

        HypothesisTests.FriedmanResult result = HypothesisTests.friedman(List.of("ok", "failing"),
                new double[][] {{1, inf}, {2, inf}, {0.5, 3}});

        assertThat(result.averageRanks()).containsExactly(1, 2);
    }

    @Test
    void holmAdjustsInInputOrderAndStaysMonotone() {
        double[] adjusted = HypothesisTests.holm(new double[] {0.04, 0.01, 0.03, 0.2});

        assertThat(adjusted).containsExactly(new double[] {0.09, 0.04, 0.09, 0.2}, within(1e-12));
    }
}
