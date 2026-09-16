package org.dmorfav.knapsack.core.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PisingerGeneratorTest {

    private static final long R = 1_000;

    @ParameterizedTest
    @EnumSource(InstanceClass.class)
    void sameSpecGeneratesTheSameInstance(InstanceClass type) {
        GeneratorSpec spec = spec(type, 7);

        assertThat(PisingerGenerator.generate(spec)).isEqualTo(PisingerGenerator.generate(spec));
        assertThat(PisingerGenerator.generate(spec)).isNotEqualTo(PisingerGenerator.generate(spec(type, 8)));
    }

    @ParameterizedTest
    @EnumSource(InstanceClass.class)
    void capacityFollowsPisingerSeriesConvention(InstanceClass type) {
        Instance instance = PisingerGenerator.generate(new GeneratorSpec(type, 200, R, 30, 100, 1));
        long totalWeight = instance.items().stream().mapToLong(Item::weight).sum();

        assertThat(instance.capacity()).isEqualTo(30 * totalWeight / 101);
        assertThat(instance.size()).isEqualTo(200);
    }

    @Test
    void uncorrelatedCoefficientsStayInRange() {
        assertThat(generate(InstanceClass.UNCORRELATED).items()).allSatisfy(item -> {
            assertThat(item.weight()).isBetween(1L, R);
            assertThat(item.profit()).isBetween(1L, R);
        });
    }

    @Test
    void weaklyCorrelatedProfitsStayNearTheirWeight() {
        assertThat(generate(InstanceClass.WEAKLY_CORRELATED).items()).allSatisfy(item -> {
            assertThat(item.weight()).isBetween(1L, R);
            assertThat(item.profit()).isBetween(Math.max(1, item.weight() - R / 10), item.weight() + R / 10);
        });
    }

    @Test
    void stronglyCorrelatedProfitIsWeightPlusTenthOfRange() {
        assertThat(generate(InstanceClass.STRONGLY_CORRELATED).items())
                .allSatisfy(item -> assertThat(item.profit()).isEqualTo(item.weight() + R / 10));
    }

    @Test
    void inverseStronglyCorrelatedWeightIsProfitPlusTenthOfRange() {
        assertThat(generate(InstanceClass.INVERSE_STRONGLY_CORRELATED).items()).allSatisfy(item -> {
            assertThat(item.profit()).isBetween(1L, R);
            assertThat(item.weight()).isEqualTo(item.profit() + R / 10);
        });
    }

    @Test
    void almostStronglyCorrelatedProfitIsWithinASmallBand() {
        assertThat(generate(InstanceClass.ALMOST_STRONGLY_CORRELATED).items()).allSatisfy(item -> assertThat(item.profit())
                .isBetween(item.weight() + R / 10 - R / 500, item.weight() + R / 10 + R / 500));
    }

    @Test
    void subsetSumProfitEqualsWeight() {
        assertThat(generate(InstanceClass.SUBSET_SUM).items())
                .allSatisfy(item -> assertThat(item.profit()).isEqualTo(item.weight()));
    }

    @Test
    void similarWeightsIgnoreTheRange() {
        assertThat(generate(InstanceClass.UNCORRELATED_SIMILAR_WEIGHTS).items()).allSatisfy(item -> {
            assertThat(item.weight()).isBetween(100_000L, 100_100L);
            assertThat(item.profit()).isBetween(1L, 1_000L);
        });
    }

    @Test
    void nameDescribesTheSpec() {
        assertThat(new GeneratorSpec(InstanceClass.SUBSET_SUM, 50, 1_000, 3, 10, 42).name())
                .isEqualTo("subset-sum-n50-r1000-h3of10-s42");
    }

    @Test
    void slugsRoundTripAndUnknownSlugsListTheOptions() {
        for (InstanceClass type : InstanceClass.values()) {
            assertThat(InstanceClass.fromSlug(type.slug())).isEqualTo(type);
        }
        assertThatThrownBy(() -> InstanceClass.fromSlug("random"))
                .hasMessageContaining("random")
                .hasMessageContaining("strongly-correlated");
    }

    @Test
    void rejectsInvalidSpecs() {
        assertThatThrownBy(() -> new GeneratorSpec(InstanceClass.UNCORRELATED, 0, R, 1, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeneratorSpec(InstanceClass.UNCORRELATED, 10, 9, 1, 1, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GeneratorSpec(InstanceClass.UNCORRELATED, 10, R, 5, 4, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Instance generate(InstanceClass type) {
        return PisingerGenerator.generate(new GeneratorSpec(type, 2_000, R, 50, 100, 3));
    }

    private static GeneratorSpec spec(InstanceClass type, long seed) {
        return new GeneratorSpec(type, 50, R, 50, 100, seed);
    }
}
