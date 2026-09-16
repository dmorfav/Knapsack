package org.dmorfav.knapsack.bench;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class OptionsTest {

    @Test
    void defaultsLimitOnlyTime() {
        Options options = Options.parse();

        assertThat(options.instance()).isEqualTo(Path.of("instances", "legacy-20.json"));
        assertThat(options.seed()).isEqualTo(1);
        assertThat(options.budget().limitsEvaluations()).isFalse();
        assertThat(options.budget().maxTime()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void parsesEveryOption() {
        Options options = Options.parse(
                "--instance=instances/legacy-100.json", "--seed=7", "--max-evaluations=500", "--max-seconds=3");

        assertThat(options.instance()).isEqualTo(Path.of("instances/legacy-100.json"));
        assertThat(options.seed()).isEqualTo(7);
        assertThat(options.budget().maxEvaluations()).isEqualTo(500);
        assertThat(options.budget().maxTime()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void rejectsUnknownOrMalformedArguments() {
        assertThatThrownBy(() -> Options.parse("--semilla=1")).hasMessageContaining("--semilla");
        assertThatThrownBy(() -> Options.parse("seed=1")).hasMessageContaining("seed=1");
        assertThatThrownBy(() -> Options.parse("--seed=uno")).hasMessageContaining("'uno'");
        assertThatThrownBy(() -> Options.parse("--max-evaluations=0")).isInstanceOf(IllegalArgumentException.class);
    }
}
