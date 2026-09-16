package org.dmorfav.knapsack.bench.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.dmorfav.knapsack.bench.execution.SolverExecutor;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.io.InstanceFiles;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ExperimentRunnerTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-16T10:15:30Z"), ZoneOffset.UTC);

    @TempDir
    Path root;

    private final AtomicInteger stepCalls = new AtomicInteger();

    /** Mete los objetos uno a uno en orden, evaluando tras cada paso: deja una traza creciente. */
    private final KnapsackSolver stepper = FakeSolver.of("fake", "stepper", (instance, context) -> {
        stepCalls.incrementAndGet();
        Solution solution = Solution.empty(instance.size());
        for (int i = 0; i < instance.size(); i++) {
            Solution next = solution.with(i, true);
            if (context.evaluate(next).feasible()) {
                solution = next;
            }
        }
        return solution;
    });

    private final KnapsackSolver broken = FakeSolver.of("fake", "broken", (instance, context) -> {
        throw new IllegalStateException("roto");
    });

    @Test
    void writesManifestRunsTracesAndSummary() throws IOException {
        InstanceFiles.write(new Instance("tiny", 5, List.of(new Item(3, 2), new Item(4, 3), new Item(5, 4)))
                .withKnownOptimum(7), root.resolve("tiny.json"));
        Experiment experiment = new Experiment("runner-test", "prueba", List.of("tiny.json"), List.of("*"), 3, 1,
                Budget.ofTime(Duration.ofSeconds(5)), 2);
        StringWriter out = new StringWriter();

        ResultsDirectory results = new ExperimentRunner(List.of(stepper, broken),
                new SolverExecutor.InProcessExecutor(), new PrintWriter(out), CLOCK)
                .run(experiment, root, root.resolve("results"));

        assertThat(results.path()).isEqualTo(root.resolve("results/runner-test/20260916-101530"));
        assertThat(stepCalls).hasValue(3 + 2);

        List<RunRecord> runs = results.readRuns();
        assertThat(runs).hasSize(6);
        RunRecord stepperRun = runs.stream().filter(run -> run.solver().equals("fake/stepper")).findFirst().orElseThrow();
        assertThat(stepperRun.status()).isEqualTo("ok");
        assertThat(stepperRun.profit()).isEqualTo(7);
        assertThat(stepperRun.gap()).isEqualTo(0);
        assertThat(stepperRun.solution()).isEqualTo("110");
        assertThat(stepperRun.evaluations()).isEqualTo(3);
        assertThat(stepperRun.evaluationsToBest()).isEqualTo(2);
        assertThat(runs).filteredOn(run -> run.solver().equals("fake/broken"))
                .allSatisfy(run -> assertThat(run.failure()).isEqualTo("excepción IllegalStateException: roto"));

        JsonMapper mapper = JsonMapper.builder().build();
        JsonNode manifest = mapper.readTree(Files.readString(results.path().resolve("manifest.json")));
        assertThat(manifest.get("finishedAt").asString()).isEqualTo("2026-09-16T10:15:30Z");
        assertThat(manifest.get("totalRuns").asInt()).isEqualTo(6);
        assertThat(manifest.get("instances").get(0).get("sha256").asString()).hasSize(64);
        assertThat(manifest.get("environment").get("javaVersion").asString()).startsWith(String.valueOf(Runtime.version().feature()));

        List<String> traces = Files.readAllLines(results.path().resolve("traces.jsonl"));
        assertThat(traces).hasSize(6);
        assertThat(traces).anyMatch(line -> line.contains("\"points\":[[1,") && line.contains(",3],[2,") && line.endsWith(",7]]}"));

        assertThat(Files.readAllLines(results.path().resolve("summary.csv"))).hasSize(3);
        assertThat(out.toString()).contains("[6/6]", "tiny (óptimo 7)", "fake/stepper", "3/3", "0/3");
    }

    @Test
    void secondRunInTheSameSecondGetsItsOwnDirectory() throws IOException {
        InstanceFiles.write(new Instance("tiny", 5, List.of(new Item(3, 2))), root.resolve("tiny.json"));
        Experiment experiment = new Experiment("twice", "", List.of("tiny.json"), List.of("fake/stepper"), 1, 1,
                Budget.ofTime(Duration.ofSeconds(5)), 0);
        ExperimentRunner runner = new ExperimentRunner(List.of(stepper), new SolverExecutor.InProcessExecutor(),
                new PrintWriter(new StringWriter()), CLOCK);

        Path first = runner.run(experiment, root, root.resolve("results")).path();
        Path second = runner.run(experiment, root, root.resolve("results")).path();

        assertThat(second).isNotEqualTo(first);
        assertThat(second.getFileName()).hasToString("20260916-101530-2");
    }
}
