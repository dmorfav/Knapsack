package org.dmorfav.knapsack.bench.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.dmorfav.knapsack.core.solver.Budget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExperimentTest {

    @TempDir
    Path tempDir;

    @Test
    void readsAFullDefinition() throws IOException {
        Experiment experiment = Experiment.read(write("""
                name: full
                description: Todo explícito
                instances: [a.json, "b/*.json"]
                solvers: ["greedy/*"]
                runs: 7
                firstSeed: 100
                warmupRuns: 2
                budget:
                  maxEvaluations: 500
                  maxSeconds: 3
                """));

        assertThat(experiment.name()).isEqualTo("full");
        assertThat(experiment.instances()).containsExactly("a.json", "b/*.json");
        assertThat(experiment.solvers()).containsExactly("greedy/*");
        assertThat(experiment.runs()).isEqualTo(7);
        assertThat(experiment.seed(0)).isEqualTo(100);
        assertThat(experiment.seed(6)).isEqualTo(106);
        assertThat(experiment.warmupRuns()).isEqualTo(2);
        assertThat(experiment.budget().maxEvaluations()).isEqualTo(500);
        assertThat(experiment.budget().maxTime()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void appliesDefaults() throws IOException {
        Experiment experiment = Experiment.read(write("""
                name: minimal
                instances: [a.json]
                runs: 1
                budget:
                  maxSeconds: 1
                """));

        assertThat(experiment.solvers()).containsExactly("*");
        assertThat(experiment.firstSeed()).isEqualTo(1);
        assertThat(experiment.warmupRuns()).isEqualTo(1);
        assertThat(experiment.budget().limitsEvaluations()).isFalse();
        assertThat(experiment.description()).isEmpty();
    }

    @Test
    void requiresATimeLimit() throws IOException {
        Path file = write("""
                name: no-time
                instances: [a.json]
                runs: 1
                budget:
                  maxEvaluations: 10
                """);

        assertThatThrownBy(() -> Experiment.read(file)).hasMessageContaining("maxSeconds");
    }

    @Test
    void rejectsUnknownFieldsAndBadNames() throws IOException {
        Path typo = write("""
                name: typo
                instances: [a.json]
                runs: 1
                semillas: 3
                budget: {maxSeconds: 1}
                """);
        Path badName = write("""
                name: Mi Experimento
                instances: [a.json]
                runs: 1
                budget: {maxSeconds: 1}
                """);

        assertThatThrownBy(() -> Experiment.read(typo)).hasMessageContaining("semillas");
        assertThatThrownBy(() -> Experiment.read(badName)).hasMessageContaining("kebab-case");
    }

    @Test
    void rejectsInvalidValues() {
        List<String> one = List.of("x");
        Budget budget = Budget.ofTime(Duration.ofSeconds(1));

        assertThatThrownBy(() -> new Experiment("x", "", one, one, 0, 1, budget, 0)).hasMessageContaining("runs");
        assertThatThrownBy(() -> new Experiment("x", "", List.of(), one, 1, 1, budget, 0)).hasMessageContaining("instances");
        assertThatThrownBy(() -> new Experiment("x", "", one, one, 1, 1, budget, -1)).hasMessageContaining("warmupRuns");
    }

    private Path write(String yaml) throws IOException {
        Path file = Files.createTempFile(tempDir, "experiment", ".yaml");
        Files.writeString(file, yaml);
        return file;
    }
}
