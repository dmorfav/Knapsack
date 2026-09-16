package org.dmorfav.knapsack.core.solver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SolverInfoTest {

    @Test
    void idJoinsFamilyAndVariant() {
        assertThat(new SolverInfo("harmony-search", "improved-v2", "Ada").id())
                .isEqualTo("harmony-search/improved-v2");
    }

    @Test
    void requiresKebabCaseIdentifiers() {
        assertThatThrownBy(() -> new SolverInfo("HarmonySearch", "v1", "Ada"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SolverInfo("harmony-search", "v1/x", "Ada"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SolverInfo("harmony-search", "v1", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
