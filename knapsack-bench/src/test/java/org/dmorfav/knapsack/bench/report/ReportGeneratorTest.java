package org.dmorfav.knapsack.bench.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import java.util.stream.Stream;

import org.dmorfav.knapsack.bench.experiment.Experiment;
import org.dmorfav.knapsack.bench.experiment.ExperimentRunner;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.io.InstanceFiles;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ReportGeneratorTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @TempDir
    Path root;

    private Path results;
    private Path output;

    @BeforeEach
    void runTwoExperiments() throws IOException {
        results = root.resolve("results");
        output = root.resolve("docs/resultados");
        InstanceFiles.write(new Instance("first", 6, List.of(new Item(4, 3), new Item(5, 4), new Item(3, 2)))
                .withKnownOptimum(8), root.resolve("first.json"));
        InstanceFiles.write(new Instance("second", 9, List.of(new Item(6, 5), new Item(5, 4), new Item(2, 1))),
                root.resolve("second.json"));
        Experiment experiment = new Experiment("report-test", "Prueba del informe", List.of("*.json"),
                List.of("*"), 3, 1, Budget.ofTime(Duration.ofSeconds(5)), 0);
        List<KnapsackSolver> solvers = List.of(solver("fake", "fill"), solver("fake", "empty"));
        PrintWriter quiet = new PrintWriter(new StringWriter());

        new ExperimentRunner(solvers, quiet, clock("2026-09-14T10:00:00Z")).run(experiment, root, results);
        new ExperimentRunner(solvers, quiet, clock("2026-09-21T10:00:00Z")).run(experiment, root, results);
    }

    @Test
    void publishesAnIndexNewestFirstWithAPagePerRun() throws IOException {
        int published = new ReportGenerator("https://example.org/repo").generate(results, output);

        assertThat(published).isEqualTo(2);
        String index = Files.readString(output.resolve("index.md"));
        assertThat(index).contains("[2026-09-21 10:00 UTC](report-test/20260921-100000/index.md)");
        assertThat(index.indexOf("2026-09-21")).isLessThan(index.indexOf("2026-09-14"));

        Path page = output.resolve("report-test/20260921-100000");
        String markdown = Files.readString(page.resolve("index.md"));
        assertThat(markdown)
                .contains("# report-test · 2026-09-21 10:00 UTC", "Prueba del informe")
                .contains("## Clasificación global", "Prueba de Friedman", "| 1 | `fake/fill` |")
                .contains("## Comparaciones por pares", "??? info \"Ver los 1 pares\"")
                .contains("### first", "óptimo 8", "### second", "cota de Dantzig")
                .contains("≤", "[manifest.json](datos/manifest.json)");
        assertThat(page.resolve("datos/runs.jsonl")).exists();
        assertThat(page.resolve("datos/manifest.json")).exists();
        assertThat(Files.readAllLines(page.resolve("datos/summary.csv"))).hasSize(5);
    }

    @Test
    void everyReferencedChartIsValidVegaLiteWithData() throws IOException {
        new ReportGenerator(null).generate(results, output);
        Path page = output.resolve("report-test/20260914-100000");
        String markdown = Files.readString(page.resolve("index.md"));

        List<String> charts = markdown.lines()
                .filter(line -> line.startsWith("<div class=\"vega-chart\""))
                .map(line -> line.substring(line.indexOf("data-src=\"") + 10, line.lastIndexOf("\"")))
                .toList();

        assertThat(charts).containsExactly("charts/perfil.json", "charts/gap-01.json", "charts/convergencia-01.json",
                "charts/gap-02.json", "charts/convergencia-02.json");
        for (String chart : charts) {
            JsonNode spec = JSON.readTree(Files.readString(page.resolve(chart)));
            assertThat(spec.get("$schema").asString()).contains("vega-lite");
            assertThat(spec.get("data").get("values").size()).as(chart).isPositive();
        }
    }

    @Test
    void evolutionPlotsEachRunOfTheExperiment() throws IOException {
        new ReportGenerator(null).generate(results, output);

        assertThat(Files.readString(output.resolve("evolucion/index.md"))).contains("## report-test");
        JsonNode spec = JSON.readTree(Files.readString(output.resolve("evolucion/charts/report-test.json")));
        JsonNode values = spec.get("data").get("values");
        assertThat(values.size()).isEqualTo(4);
        assertThat(values.get(0).get("suite").asString()).hasSize(8);
    }

    @Test
    void regeneratingReplacesThePreviousOutput() throws IOException {
        ReportGenerator generator = new ReportGenerator(null);
        generator.generate(results, output);
        Files.writeString(output.resolve("stale.md"), "viejo");

        generator.generate(results, output);

        assertThat(output.resolve("stale.md")).doesNotExist();
        assertThat(output.resolve(ReportGenerator.MARKER)).exists();
    }

    @Test
    void refusesToWipeAFolderItDidNotGenerate() throws IOException {
        Files.createDirectories(output);
        Files.writeString(output.resolve("importante.md"), "no borrar");

        assertThatThrownBy(() -> new ReportGenerator(null).generate(results, output))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("no se borra");
        assertThat(output.resolve("importante.md")).exists();
    }

    @Test
    void withoutResultsPublishesPlaceholderPages() throws IOException {
        int published = new ReportGenerator(null).generate(root.resolve("nothing"), output);

        assertThat(published).isZero();
        assertThat(Files.readString(output.resolve("index.md"))).contains("Todavía no hay resultados publicados.");
        assertThat(Files.readString(output.resolve("evolucion/index.md"))).contains("Todavía no hay resultados publicados.");
    }

    @Test
    void convergenceCurveNeverRisesAndCountsRunsWithoutSolutionsAsEmpty() {
        List<List<long[]>> traces = List.of(
                List.of(new long[] {1, 0, 50}, new long[] {10, 0, 90}),
                List.of(new long[] {5, 0, 100}),
                List.of());

        List<Convergence.Point> curve = Convergence.medianCurve(traces, 100);

        assertThat(curve.getFirst().evaluations()).isEqualTo(1);
        assertThat(curve.getFirst().gap()).isEqualTo(100);
        assertThat(curve.getLast().evaluations()).isEqualTo(10);
        assertThat(curve.getLast().gap()).isEqualTo(10);
        assertThat(curve).allSatisfy(point -> assertThat(point.runs()).isEqualTo(3));
        for (int i = 1; i < curve.size(); i++) {
            assertThat(curve.get(i).gap()).isLessThanOrEqualTo(curve.get(i - 1).gap());
        }
        assertThat(Convergence.checkpoints(1_000_000)).startsWith(1L).endsWith(1_000_000L).hasSizeLessThanOrEqualTo(41);
    }

    private static Clock clock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    /** "fill" mete objetos en orden mientras quepan; "empty" devuelve la mochila vacía. */
    private static KnapsackSolver solver(String family, String variant) {
        SolverInfo info = new SolverInfo(family, variant, "Test");
        return new KnapsackSolver() {
            @Override
            public SolverInfo info() {
                return info;
            }

            @Override
            public Solution solve(Instance instance, SolverContext context) {
                Solution solution = Solution.empty(instance.size());
                if (variant.equals("fill")) {
                    for (int i = 0; i < instance.size(); i++) {
                        Solution next = solution.with(i, true);
                        if (context.evaluate(next).feasible()) {
                            solution = next;
                        }
                    }
                }
                context.evaluate(solution);
                return solution;
            }
        };
    }
}
