package org.dmorfav.knapsack.bench.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

import org.dmorfav.knapsack.baselines.DynamicProgrammingSolver;
import org.dmorfav.knapsack.bench.legacy.LegacySolvers;
import org.junit.jupiter.api.Test;

class SolverProvenanceTest {

    /** Surefire ejecuta los tests desde knapsack-bench. */
    private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

    @Test
    void followsTheCodeTheSolverUsesAndOnlyProjectSources() {
        SolverProvenance provenance = SolverProvenance.of(DynamicProgrammingSolver.class, ROOT);

        assertThat(provenance.sources())
                .contains("knapsack-baselines/src/main/java/org/dmorfav/knapsack/baselines/DynamicProgrammingSolver.java",
                        "knapsack-core/src/main/java/org/dmorfav/knapsack/core/Solution.java",
                        "knapsack-core/src/main/java/org/dmorfav/knapsack/core/solver/SolverInfo.java")
                .noneMatch(source -> source.contains("GreedySolver"))
                .noneMatch(source -> source.startsWith("bytecode:"));
        assertThat(provenance.codeHash()).hasSize(64);
        assertThat(SolverProvenance.of(DynamicProgrammingSolver.class, ROOT).codeHash()).isEqualTo(provenance.codeHash());
    }

    @Test
    void legacyAdapterDependsOnItsOwnAlgorithmButNotOnItsNestedSiblings() {
        SolverProvenance provenance = SolverProvenance.of(LegacySolvers.LegacyHarmonySearch.class, ROOT);

        assertThat(provenance.sources())
                .contains("knapsack-algorithms/src/main/java/algorithms/HarmonySearch.java",
                        "knapsack-bench/src/main/java/org/dmorfav/knapsack/bench/legacy/CountingKnapsack.java")
                .doesNotContain("knapsack-algorithms/src/main/java/algorithms/HillClimbingBestAscent.java",
                        "knapsack-algorithms/src/main/java/algorithms/ImprovedHarmonySearch.java");
        assertThat(SolverProvenance.of(LegacySolvers.LegacyHillClimbingBestAscent.class, ROOT).codeHash())
                .isNotEqualTo(provenance.codeHash());
    }

    @Test
    void gitAuthorsExcludeTheFrameworkModules() {
        SolverProvenance provenance = SolverProvenance.of(DynamicProgrammingSolver.class, ROOT);

        if (Files.isDirectory(ROOT.resolve(".git"))) {
            assertThat(provenance.gitAuthors()).isNotEmpty();
            assertThat(provenance.uncommittedChanges()).isNotNull();
        }
        // Un solver cuyas fuentes son todas de core o bench no tiene autores propios
        assertThat(SolverProvenance.of(org.dmorfav.knapsack.bench.SolverRun.class, ROOT).gitAuthors()).isEmpty();
    }

    @Test
    void hashChangesWithContentButNotWithLineEndings() {
        String unix = SolverProvenance.hash(contents("a.java", "class A {\n}\n"));

        assertThat(SolverProvenance.hash(contents("a.java", "class A {\r\n}\r\n"))).isEqualTo(unix);
        assertThat(SolverProvenance.hash(contents("a.java", "class A { }\n"))).isNotEqualTo(unix);
        assertThat(SolverProvenance.hash(contents("b.java", "class A {\n}\n"))).isNotEqualTo(unix);
    }

    private static TreeMap<String, byte[]> contents(String name, String content) {
        TreeMap<String, byte[]> contents = new TreeMap<>();
        contents.put(name, content.getBytes(StandardCharsets.UTF_8));
        return contents;
    }
}
