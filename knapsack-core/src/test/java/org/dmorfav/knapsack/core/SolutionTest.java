package org.dmorfav.knapsack.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SolutionTest {

    @Test
    void emptySolutionSelectsNothing() {
        Solution solution = Solution.empty(4);

        assertThat(solution.size()).isEqualTo(4);
        assertThat(solution.selectedCount()).isZero();
        assertThat(solution.nextSelected(0)).isEqualTo(-1);
        assertThat(solution).hasToString("0000");
    }

    @Test
    void bitsRoundTrip() {
        Solution solution = Solution.fromBits(1, 0, 1, 1, 0);

        assertThat(solution.toBits()).containsExactly(1, 0, 1, 1, 0);
        assertThat(solution.selectedIndices()).containsExactly(0, 2, 3);
        assertThat(solution.selectedCount()).isEqualTo(3);
        assertThat(solution).isEqualTo(Solution.of(true, false, true, true, false));
    }

    @Test
    void rejectsBitsOtherThanZeroOrOne() {
        assertThatThrownBy(() -> Solution.fromBits(0, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("posición 1");
    }

    @Test
    void withAndFlipReturnCopies() {
        Solution original = Solution.fromBits(0, 0, 0);

        Solution flipped = original.flip(1);
        Solution set = flipped.with(2, true);

        assertThat(original).hasToString("000");
        assertThat(flipped).hasToString("010");
        assertThat(set).hasToString("011");
        assertThat(set.flip(1)).hasToString("001");
    }

    @Test
    void trailingUnselectedPositionsCountForEquality() {
        assertThat(Solution.fromBits(1, 0)).isNotEqualTo(Solution.fromBits(1, 0, 0));
        assertThat(Solution.fromBits(1, 0)).hasSameHashCodeAs(Solution.of(true, false));
    }

    @Test
    void rejectsIndexesOutOfRange() {
        Solution solution = Solution.empty(3);

        assertThatThrownBy(() -> solution.isSelected(3)).isInstanceOf(IndexOutOfBoundsException.class);
        assertThatThrownBy(() -> solution.with(-1, true)).isInstanceOf(IndexOutOfBoundsException.class);
    }
}
