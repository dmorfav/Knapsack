package org.dmorfav.knapsack.bench.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.io.InstanceFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommandsTest {

    @TempDir
    Path tempDir;

    @Test
    void withoutSubcommandPrintsUsage() {
        CliResult result = CliResult.execute();

        assertThat(result.exitCode()).isZero();
        assertThat(result.out()).contains("run", "generate", "optimum");
    }

    @Test
    void runExecutesBaselinesAndLegacySolvers() throws IOException {
        Path file = write(new Instance("cli", 15, List.of(
                new Item(10, 5), new Item(7, 4), new Item(8, 6), new Item(4, 3), new Item(3, 2), new Item(9, 7))));

        CliResult result = CliResult.execute("run", "--instance=" + file, "--seed=3", "--max-seconds=10");

        assertThat(result.exitCode()).isZero();
        assertThat(result.out())
                .contains("Instancia cli: 6 objetos, capacidad 15, óptimo desconocido (cota de Dantzig")
                .contains("exact/dynamic-programming (baseline)", "greedy/ratio (baseline)", "greedy/ratio-best-item (baseline)")
                .contains("harmony-search/legacy (Jorge Herrera Franklin)", "hill-climbing-best-ascent/legacy (Daniel Morfa Vega)")
                .contains("gap ≤");
    }

    @Test
    void runReportsGapAgainstTheKnownOptimum() throws IOException {
        Path file = write(new Instance("known", 10, List.of(new Item(6, 5), new Item(5, 5), new Item(9, 8)))
                .withKnownOptimum(11));

        CliResult result = CliResult.execute("run", "--instance=" + file, "--max-seconds=10");

        assertThat(result.out()).contains("óptimo 11").contains("beneficio 11 · peso 10/10 · 2 objetos · gap 0.00 %");
    }

    @Test
    void generateWritesAnInstanceWithItsOptimum() throws IOException {
        Path target = tempDir.resolve("nested/generated.json");

        CliResult result = CliResult.execute("generate", "--class=strongly-correlated", "--items=40", "--range=100",
                "--seed=9", "--output=" + target);

        Instance instance = InstanceFiles.read(target);
        assertThat(result.exitCode()).isZero();
        assertThat(instance.name()).isEqualTo("strongly-correlated-n40-r100-h50of100-s9");
        assertThat(instance.size()).isEqualTo(40);
        assertThat(instance.knownOptimum()).isPresent();
        assertThat(result.out()).contains("óptimo " + instance.knownOptimum().getAsLong());
    }

    @Test
    void generateCanSkipTheOptimum() throws IOException {
        Path target = tempDir.resolve("no-optimum.json");

        CliResult result = CliResult.execute("generate", "--class=subset-sum", "--no-optimum", "--output=" + target);

        assertThat(result.exitCode()).isZero();
        assertThat(InstanceFiles.read(target).knownOptimum()).isEmpty();
    }

    @Test
    void generateRejectsUnknownClasses() {
        CliResult result = CliResult.execute("generate", "--class=random");

        assertThat(result.exitCode()).isNotZero();
        assertThat(result.err()).contains("random", "uncorrelated-similar-weights");
    }

    @Test
    void optimumWritesMissingOptimaAndDetectsDiscrepancies() throws IOException {
        Instance base = new Instance("check", 10, List.of(new Item(6, 5), new Item(5, 5), new Item(9, 8)));
        Path missing = write(base);
        Path wrong = tempDir.resolve("wrong.json");
        InstanceFiles.write(base.withKnownOptimum(99), wrong);

        CliResult preview = CliResult.execute("optimum", missing.toString());
        CliResult written = CliResult.execute("optimum", "--write", missing.toString());
        CliResult checked = CliResult.execute("optimum", missing.toString(), wrong.toString());

        assertThat(preview.out()).contains("óptimo 11 (usa --write");
        assertThat(InstanceFiles.read(missing).knownOptimum()).hasValue(11);
        assertThat(written.out()).contains("guardado");
        assertThat(checked.exitCode()).isEqualTo(1);
        assertThat(checked.out()).contains("coincide", "DISCREPANCIA, declara 99 y el óptimo es 11");
    }

    @Test
    void suiteGeneratesTheCartesianProductWithOptimaAndChecksIt() throws IOException {
        Path suite = writeSuite("pequena", "[uncorrelated, subset-sum]", "[10]", "[25, 75]");

        CliResult generated = CliResult.execute("suite", suite.toString());
        CliResult checked = CliResult.execute("suite", "--check", suite.toString());

        assertThat(generated.exitCode()).isZero();
        assertThat(generated.out()).contains("Suite pequena: 4 instancias generadas");
        Path file = suite.resolveSibling("subset-sum-n10-r100-h75of100-s3.json");
        assertThat(InstanceFiles.read(file).knownOptimum()).isPresent();
        assertThat(checked.exitCode()).isZero();
        assertThat(checked.out()).contains("4 instancias comprobadas").doesNotContain("problemas");
    }

    @Test
    void suiteCheckDetectsEditedMissingAndExtraFiles() throws IOException {
        Path suite = writeSuite("vigilada", "[uncorrelated]", "[10]", "[25, 75]");
        CliResult.execute("suite", suite.toString());
        Path edited = suite.resolveSibling("uncorrelated-n10-r100-h25of100-s3.json");
        Instance original = InstanceFiles.read(edited);
        InstanceFiles.write(original.withKnownOptimum(original.knownOptimum().getAsLong() + 1), edited);
        Files.delete(suite.resolveSibling("uncorrelated-n10-r100-h75of100-s3.json"));
        Files.writeString(suite.resolveSibling("intrusa.json"), "{}");

        CliResult checked = CliResult.execute("suite", "--check", suite.toString());

        assertThat(checked.exitCode()).isEqualTo(1);
        assertThat(checked.out()).contains("uncorrelated-n10-r100-h25of100-s3.json: no coincide con la salida del generador",
                "uncorrelated-n10-r100-h75of100-s3.json: falta el fichero", "intrusa.json: sobra", "3 problemas");
    }

    @Test
    void suiteRequiresAnOptimumForEveryInstance() throws IOException {
        Path suite = writeSuite("sin-optimo", "[uncorrelated-similar-weights]", "[200]", "[50]");

        CliResult result = CliResult.execute("suite", suite.toString());

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.out()).contains("sin óptimo calculable");
    }

    private Path writeSuite(String name, String classes, String items, String instanceNumbers) throws IOException {
        Path directory = Files.createDirectories(tempDir.resolve(name));
        Path suite = directory.resolve("suite.yaml");
        Files.writeString(suite, """
                name: %s
                classes: %s
                items: %s
                range: 100
                instanceNumbers: %s
                series: 100
                seeds: [3]
                """.formatted(name, classes, items, instanceNumbers));
        return suite;
    }

    @Test
    void experimentThenSummarizeProduceTheSameSummary() throws IOException {
        Path instance = write(new Instance("exp", 10, List.of(new Item(6, 5), new Item(5, 5), new Item(9, 8)))
                .withKnownOptimum(11));
        Path yaml = tempDir.resolve("exp.yaml");
        Files.writeString(yaml, """
                name: cli-exp
                instances: ["%s"]
                solvers: ["greedy/*", "exact/*"]
                runs: 2
                warmupRuns: 0
                budget:
                  maxSeconds: 5
                """.formatted(instance.toString().replace('\\', '/')));
        Path output = tempDir.resolve("results");

        CliResult experiment = CliResult.execute("experiment", yaml.toString(), "--output=" + output);
        Path resultsDir;
        try (var dirs = Files.list(output.resolve("cli-exp"))) {
            resultsDir = dirs.findFirst().orElseThrow();
        }
        String firstSummary = Files.readString(resultsDir.resolve("summary.csv"));
        Files.delete(resultsDir.resolve("summary.csv"));
        CliResult summarize = CliResult.execute("summarize", resultsDir.toString());

        assertThat(experiment.exitCode()).isZero();
        assertThat(experiment.out()).contains("1 instancias × 3 solvers × 2 ejecuciones = 6", "[6/6]", "exp (óptimo 11)");
        assertThat(Files.readAllLines(resultsDir.resolve("runs.jsonl"))).hasSize(6);
        assertThat(summarize.exitCode()).isZero();
        assertThat(Files.readString(resultsDir.resolve("summary.csv"))).isEqualTo(firstSummary);
        assertThat(summarize.out()).contains("exact/dynamic-programming", "2/2");
    }

    private Path write(Instance instance) throws IOException {
        Path file = tempDir.resolve(instance.name() + ".json");
        Files.createDirectories(file.getParent());
        InstanceFiles.write(instance, file);
        return file;
    }
}
