package org.dmorfav.knapsack.bench.experiment;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.dmorfav.knapsack.core.solver.Budget;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Definición de un experimento: qué solvers, sobre qué instancias, cuántas ejecuciones y con qué presupuesto.
 * El límite de tiempo es obligatorio para que ningún solver bloquee el experimento.
 */
public record Experiment(String name, String description, List<String> instances, List<String> solvers, int runs,
                         long firstSeed, Budget budget, int warmupRuns) {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final YAMLMapper MAPPER = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public Experiment {
        Objects.requireNonNull(name, "name");
        if (!SLUG.matcher(name).matches()) {
            throw new IllegalArgumentException("name debe ser kebab-case en minúsculas, es '" + name + "'");
        }
        description = description == null ? "" : description;
        instances = List.copyOf(instances);
        if (instances.isEmpty()) {
            throw new IllegalArgumentException("instances necesita al menos un patrón");
        }
        solvers = List.copyOf(solvers);
        if (solvers.isEmpty()) {
            throw new IllegalArgumentException("solvers necesita al menos un patrón");
        }
        if (runs < 1) {
            throw new IllegalArgumentException("runs debe ser >= 1, es " + runs);
        }
        Objects.requireNonNull(budget, "budget");
        if (!budget.limitsTime()) {
            throw new IllegalArgumentException("budget.maxSeconds es obligatorio");
        }
        if (warmupRuns < 0) {
            throw new IllegalArgumentException("warmupRuns debe ser >= 0, es " + warmupRuns);
        }
    }

    public static Experiment read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return MAPPER.readValue(reader, ExperimentFile.class).toExperiment();
        } catch (JacksonException | IllegalArgumentException e) {
            throw new IOException("experimento inválido en " + path + ": " + e.getMessage(), e);
        }
    }

    /** Semillas de las ejecuciones: firstSeed, firstSeed + 1, ... */
    public long seed(int runIndex) {
        return firstSeed + runIndex;
    }

    record ExperimentFile(String name, String description, List<String> instances, List<String> solvers, Integer runs,
                          Long firstSeed, BudgetFile budget, Integer warmupRuns) {

        Experiment toExperiment() {
            if (name == null) {
                throw new IllegalArgumentException("falta 'name'");
            }
            if (budget == null || budget.maxSeconds() == null) {
                throw new IllegalArgumentException("budget.maxSeconds es obligatorio");
            }
            Budget parsed = Budget.ofTime(Duration.ofSeconds(budget.maxSeconds()));
            if (budget.maxEvaluations() != null) {
                parsed = parsed.withMaxEvaluations(budget.maxEvaluations());
            }
            if (instances == null) {
                throw new IllegalArgumentException("falta 'instances'");
            }
            if (runs == null) {
                throw new IllegalArgumentException("falta 'runs'");
            }
            return new Experiment(name, description, instances, solvers == null ? List.of("*") : solvers, runs,
                    firstSeed == null ? 1 : firstSeed, parsed, warmupRuns == null ? 1 : warmupRuns);
        }
    }

    record BudgetFile(Long maxEvaluations, Long maxSeconds) {
    }
}
