package org.dmorfav.knapsack.bench.experiment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.io.InstanceFiles;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExperimentPlanTest {

    @TempDir
    Path root;

    private final List<KnapsackSolver> available = List.of(
            FakeSolver.of("greedy", "ratio", (instance, context) -> Solution.empty(instance.size())),
            FakeSolver.of("greedy", "ratio-best-item", (instance, context) -> Solution.empty(instance.size())),
            FakeSolver.of("harmony-search", "legacy", (instance, context) -> Solution.empty(instance.size())));

    @BeforeEach
    void createInstances() throws IOException {
        for (String file : List.of("instances/top.json", "instances/pisinger/a.json", "instances/pisinger/b.json",
                "instances/pisinger/deep/c.json")) {
            Path path = root.resolve(file);
            Files.createDirectories(path.getParent());
            InstanceFiles.write(new Instance(path.getFileName().toString().replace(".json", ""), 5,
                    List.of(new Item(1, 1))), path);
        }
        Files.writeString(root.resolve("instances/pisinger/notes.txt"), "no es una instancia");
    }

    @Test
    void singleStarStaysInOneDirectory() throws IOException {
        assertThat(ExperimentPlan.resolveInstanceFiles(root, List.of("instances/pisinger/*.json")))
                .containsExactly("instances/pisinger/a.json", "instances/pisinger/b.json");
    }

    @Test
    void doubleStarCrossesDirectoriesIncludingNone() throws IOException {
        assertThat(ExperimentPlan.resolveInstanceFiles(root, List.of("instances/**/*.json")))
                .containsExactly("instances/pisinger/a.json", "instances/pisinger/b.json",
                        "instances/pisinger/deep/c.json", "instances/top.json");
    }

    @Test
    void literalPathsAndDuplicatesKeepFirstOrder() throws IOException {
        assertThat(ExperimentPlan.resolveInstanceFiles(root,
                List.of("instances/top.json", "instances/pisinger/*.json", "instances/pisinger/a.json")))
                .containsExactly("instances/top.json", "instances/pisinger/a.json", "instances/pisinger/b.json");
    }

    @Test
    void patternsThatMatchNothingAreErrors() {
        assertThatThrownBy(() -> ExperimentPlan.resolveInstanceFiles(root, List.of("instances/missing.json")))
                .hasMessageContaining("instances/missing.json");
        assertThatThrownBy(() -> ExperimentPlan.resolveInstanceFiles(root, List.of("nowhere/*.json")))
                .hasMessageContaining("nowhere/*.json");
    }

    @Test
    void selectsSolversByIdPattern() {
        assertThat(ids(ExperimentPlan.selectSolvers(available, List.of("*"))))
                .containsExactly("greedy/ratio", "greedy/ratio-best-item", "harmony-search/legacy");
        assertThat(ids(ExperimentPlan.selectSolvers(available, List.of("greedy/*"))))
                .containsExactly("greedy/ratio", "greedy/ratio-best-item");
        assertThat(ids(ExperimentPlan.selectSolvers(available, List.of("*/legacy", "greedy/ratio"))))
                .containsExactly("greedy/ratio", "harmony-search/legacy");
        assertThatThrownBy(() -> ExperimentPlan.selectSolvers(available, List.of("exact/*")))
                .hasMessageContaining("exact/*");
    }

    @Test
    void plansEveryCombinationInAReproducibleShuffledOrder() throws IOException {
        Experiment experiment = new Experiment("plan", "", List.of("instances/pisinger/*.json"), List.of("greedy/*"),
                3, 10, Budget.ofTime(Duration.ofSeconds(1)), 0);

        ExperimentPlan plan = ExperimentPlan.resolve(experiment, root, available);
        ExperimentPlan again = ExperimentPlan.resolve(experiment, root, available);

        assertThat(plan.tasks()).hasSize(2 * 2 * 3);
        assertThat(plan.tasks()).extracting(ExperimentPlan.Task::seed).containsOnly(10L, 11L, 12L);
        assertThat(describe(plan)).isEqualTo(describe(again));
        assertThat(describe(plan)).isNotEqualTo(describe(plan).stream().sorted().toList());
        assertThat(plan.instances()).extracting(ExperimentPlan.PlannedInstance::sha256).allMatch(hash -> hash.length() == 64);
    }

    private static List<String> ids(List<KnapsackSolver> solvers) {
        return solvers.stream().map(solver -> solver.info().id()).toList();
    }

    private static List<String> describe(ExperimentPlan plan) {
        return plan.tasks().stream()
                .map(task -> task.instance().file() + " " + task.solver().info().id() + " " + task.seed())
                .toList();
    }
}
