package org.dmorfav.knapsack.bench.report;

import static org.dmorfav.knapsack.bench.report.ChartSpecs.obj;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.dmorfav.knapsack.bench.experiment.EnvironmentInfo;
import org.dmorfav.knapsack.bench.experiment.Manifest;
import org.dmorfav.knapsack.bench.experiment.ResultsDirectory;
import org.dmorfav.knapsack.bench.experiment.RunRecord;
import org.dmorfav.knapsack.bench.experiment.TraceRecord;
import org.dmorfav.knapsack.bench.stats.Comparison;
import org.dmorfav.knapsack.bench.stats.Summarizer;
import org.dmorfav.knapsack.bench.stats.SummaryOutput;
import org.dmorfav.knapsack.bench.stats.SummaryRow;
import tools.jackson.databind.json.JsonMapper;

/**
 * Genera las páginas MkDocs de resultados a partir de las carpetas de experimentos: un índice, una página por
 * ejecución con tablas, pruebas estadísticas y gráficas, y la evolución de cada experimento en el tiempo.
 */
public final class ReportGenerator {

    static final String MARKER = ".generado-por-knapsack-report";

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final DateTimeFormatter HUMAN_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);
    private static final String CHART = "<div class=\"vega-chart\" data-src=\"%s\"></div>%n%n";

    private final String repositoryUrl;

    /** {@code repositoryUrl} (sin barra final) enlaza los commits; puede ser null. */
    public ReportGenerator(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    /** Sustituye por completo {@code output}; se niega si la carpeta existe con contenido que no generó él. */
    public int generate(Path resultsRoot, Path output) throws IOException {
        prepareOutput(output);
        List<StoredExperiment> experiments = StoredExperiment.discover(resultsRoot);
        for (StoredExperiment experiment : experiments) {
            writeExperimentPage(experiment, previousRun(experiments, experiment),
                    output.resolve(experiment.name()).resolve(experiment.runId()));
        }
        writeIndex(experiments, output);
        writeEvolution(experiments, output.resolve("evolucion"));
        Files.writeString(output.resolve(MARKER), "Carpeta generada por el subcomando report; no la edites a mano.\n");
        return experiments.size();
    }

    private void writeExperimentPage(StoredExperiment experiment, StoredExperiment previous, Path directory)
            throws IOException {
        Files.createDirectories(directory.resolve("charts"));
        Files.createDirectories(directory.resolve("datos"));
        Manifest manifest = experiment.manifest();
        List<RunRecord> runs = experiment.runs();
        List<SummaryRow> summary = Summarizer.summarize(runs);
        Comparison comparison = Comparison.of(runs);
        Map<String, List<TraceRecord>> traces = experiment.directory().readTraces().stream()
                .collect(Collectors.groupingBy(TraceRecord::instance));

        Files.copy(experiment.directory().path().resolve(ResultsDirectory.MANIFEST),
                directory.resolve("datos").resolve(ResultsDirectory.MANIFEST), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(experiment.directory().path().resolve(ResultsDirectory.RUNS),
                directory.resolve("datos").resolve(ResultsDirectory.RUNS), StandardCopyOption.REPLACE_EXISTING);
        SummaryOutput.writeCsv(summary, directory.resolve("datos").resolve(ResultsDirectory.SUMMARY));

        StringBuilder md = new StringBuilder();
        line(md, "# %s · %s", manifest.experiment(), HUMAN_DATE.format(experiment.startedAt()));
        if (!manifest.description().isBlank()) {
            line(md, "%s", manifest.description());
        }
        writeDetails(md, experiment);
        writeSolvers(md, experiment, previous);
        md.append("""
                !!! note "Cómo leer esta página"
                    El **GAP** es `100 · (referencia − beneficio) / referencia`: la referencia es el óptimo conocido o, si falta, \
                la cota de Dantzig, y entonces el GAP se marca con ≤. Las métricas de calidad usan solo las ejecuciones \
                correctas. Los **tiempos** solo son comparables dentro de esta misma ejecución: cambian con la máquina.

                """);

        writeRanking(md, experiment, comparison, directory);
        writePairs(md, comparison);

        line(md, "## Resultados por instancia");
        List<Manifest.InstanceEntry> instances = manifest.instances();
        for (int i = 0; i < instances.size(); i++) {
            writeInstance(md, instances.get(i), String.format(Locale.ROOT, "%02d", i + 1), summary, runs,
                    traces.getOrDefault(instances.get(i).name(), List.of()), directory);
        }
        Files.writeString(directory.resolve("index.md"), md.toString());
    }

    private void writeDetails(StringBuilder md, StoredExperiment experiment) {
        Manifest manifest = experiment.manifest();
        EnvironmentInfo environment = manifest.environment();
        long failed = experiment.runs().stream().filter(run -> !run.succeeded()).count();
        line(md, "| Parámetro | Valor |");
        line(md, "|---|---|");
        row(md, "Commit", commit(environment));
        row(md, "Ejecuciones", String.format(Locale.ROOT, "%d (%d instancias × %d solvers × %d semillas desde %d), %d fallidas",
                manifest.totalRuns(), manifest.instances().size(), manifest.solvers().size(), manifest.runs(),
                manifest.firstSeed(), failed));
        row(md, "Presupuesto por ejecución", (manifest.maxEvaluations() == null ? "evaluaciones sin límite"
                : manifest.maxEvaluations() + " evaluaciones") + ", " + manifest.maxSeconds() + " s");
        row(md, "Calentamiento por solver", manifest.warmupRuns() + " (no se registran)");
        row(md, "Orden", manifest.runOrder());
        row(md, "Ejecución", manifest.execution() == null ? "no registrada (manifiesto anterior)" : manifest.execution());
        row(md, "Entorno", String.join(" · ", Stream.of(
                        environment.cpuModel(),
                        environment.availableProcessors() + " núcleos",
                        "Java " + environment.javaVersion() + " (" + environment.javaVendor() + ")",
                        environment.os() + " " + environment.arch())
                .filter(part -> part != null && !part.startsWith("null")).toList()));
        if (manifest.finishedAt() != null) {
            Duration duration = Duration.between(experiment.startedAt(), Instant.parse(manifest.finishedAt()));
            row(md, "Duración", duration.toSeconds() + " s");
        }
        row(md, "Conjunto de instancias", "`" + experiment.suite() + "`");
        row(md, "Datos", "[manifest.json](datos/manifest.json) · [runs.jsonl](datos/runs.jsonl) · [summary.csv](datos/summary.csv)");
        md.append('\n');
    }

    private void writeSolvers(StringBuilder md, StoredExperiment experiment, StoredExperiment previous) {
        List<String> changed = previous == null ? List.of() : experiment.codeChangedSince(previous);
        line(md, "## Solvers");
        if (!changed.isEmpty()) {
            md.append(String.format(Locale.ROOT, """
                    !!! warning "Código cambiado desde la ejecución anterior (%s)"
                        %s cambió de huella con la misma variante: sus resultados no son comparables con los anteriores. \
                    Un cambio de comportamiento debería publicarse como variante nueva.

                    """, HUMAN_DATE.format(previous.startedAt()),
                    changed.stream().map(id -> "`" + id + "`").collect(Collectors.joining(", "))));
        }
        line(md, "| Solver | Autor declarado | Autores en git | Código |");
        line(md, "|---|---|---|---|");
        for (Manifest.SolverEntry solver : experiment.manifest().solvers()) {
            String code;
            if (solver.codeHash() == null) {
                code = "no registrado";
            } else {
                code = "`" + solver.codeHash().substring(0, 8) + "`";
                if (changed.contains(solver.id())) {
                    code += " cambió";
                }
                if (Boolean.TRUE.equals(solver.uncommittedChanges())) {
                    code += " (sin commitear)";
                }
            }
            String gitAuthors = solver.gitAuthors() == null ? "desconocidos"
                    : solver.gitAuthors().isEmpty() ? "—" : String.join(", ", solver.gitAuthors());
            md.append(String.format(Locale.ROOT, "| `%s` | %s | %s | %s |%n", solver.id(), escape(solver.author()),
                    escape(gitAuthors), code));
        }
        md.append('\n');
        line(md, "La huella es el SHA-256 de las fuentes de las que depende cada solver; la lista completa está en "
                + "[manifest.json](datos/manifest.json). Los autores en git son los de sus fuentes, sin knapsack-core "
                + "ni knapsack-bench.");
    }

    private static StoredExperiment previousRun(List<StoredExperiment> experiments, StoredExperiment experiment) {
        // experiments va de la más reciente a la más antigua
        return experiments.stream()
                .filter(candidate -> candidate.name().equals(experiment.name()))
                .filter(candidate -> candidate.startedAt().isBefore(experiment.startedAt()))
                .findFirst()
                .orElse(null);
    }

    private void writeRanking(StringBuilder md, StoredExperiment experiment, Comparison comparison, Path directory)
            throws IOException {
        line(md, "## Clasificación global");
        if (comparison.friedman() == null) {
            line(md, "%s.", comparison.friedmanSkippedReason());
            return;
        }
        Comparison.Friedman friedman = comparison.friedman();
        line(md, "Prueba de Friedman sobre el GAP mediano de cada solver en cada instancia: χ² = %s, p = %s. %s",
                number(friedman.statistic(), 3), pValue(friedman.pValue()),
                friedman.pValue() < Comparison.ALPHA
                        ? "**Hay diferencias significativas** entre los solvers (α = 0.05)."
                        : "No hay diferencias significativas entre los solvers (α = 0.05).");
        Map<String, String> authors = experiment.manifest().solvers().stream()
                .collect(Collectors.toMap(Manifest.SolverEntry::id, Manifest.SolverEntry::author));
        line(md, "| Posición | Solver | Autor | Rango medio | Media de GAP medianos |");
        line(md, "|---:|---|---|---:|---:|");
        Integer[] order = IntStream.range(0, comparison.solvers().size()).boxed()
                .sorted(Comparator.comparingDouble(s -> friedman.averageRanks()[s]))
                .toArray(Integer[]::new);
        for (int position = 0; position < order.length; position++) {
            int s = order[position];
            String solver = comparison.solvers().get(s);
            line(md, "| %d | `%s` | %s | %s | %s |", position + 1, solver, escape(authors.getOrDefault(solver, "")),
                    number(friedman.averageRanks()[s], 2), meanOfMedianGaps(comparison, s));
        }
        md.append('\n');
        if (friedman.criticalDifference() != null) {
            line(md, "Diferencia crítica de Nemenyi (α = 0.05): **%s**. Dos solvers cuyos rangos medios difieren menos "
                    + "que ese valor no se distinguen estadísticamente.", number(friedman.criticalDifference(), 3));
        }
        List<Map<String, Object>> profile = comparison.performanceProfile().stream()
                .map(point -> obj("solver", point.solver(), "tau", point.tau(), "fraction", point.fraction()))
                .toList();
        writeChart(md, directory, "charts/perfil.json", ChartSpecs.performanceProfile(profile));
    }

    private void writePairs(StringBuilder md, Comparison comparison) {
        if (comparison.pairs().isEmpty()) {
            return;
        }
        line(md, "## Comparaciones por pares");
        long significant = comparison.pairs().stream().filter(pair -> pair.better() != null).count();
        line(md, "Prueba de Wilcoxon de rangos con signo sobre los GAP medianos por instancia, con corrección de Holm "
                + "para %d comparaciones. %d pares muestran una diferencia significativa (α = 0.05).",
                comparison.pairs().size(), significant);
        line(md, "??? info \"Ver los %d pares\"", comparison.pairs().size());
        line(md, "    | Solver A | Solver B | A gana / pierde / empata | p | p (Holm) | Conclusión |");
        line(md, "    |---|---|:---:|---:|---:|---|");
        for (Comparison.Pair pair : comparison.pairs()) {
            String conclusion = pair.better() == null ? "sin diferencia significativa" : "mejor `" + pair.better() + "`";
            line(md, "    | `%s` | `%s` | %d / %d / %d | %s%s | %s | %s |", pair.first(), pair.second(), pair.wins(),
                    pair.losses(), pair.ties(), pValue(pair.pValue()), pair.exact() ? "" : " ≈", pValue(pair.holmPValue()),
                    conclusion);
        }
        md.append('\n');
    }

    private void writeInstance(StringBuilder md, Manifest.InstanceEntry instance, String chartId, List<SummaryRow> summary,
                               List<RunRecord> runs, List<TraceRecord> traces, Path directory) throws IOException {
        boolean optimum = "OPTIMUM".equals(instance.referenceKind());
        String prefix = optimum ? "" : "≤";
        line(md, "### %s", instance.name());
        line(md, "%d objetos · capacidad %d · %s %d · `%s`", instance.items(), instance.capacity(),
                optimum ? "óptimo" : "cota de Dantzig", instance.reference(), instance.file());
        line(md, "| Solver | Correctas | Mejor | GAP mediano | GAP mínimo | Óptimos | Evaluaciones | Hasta el mejor | ms |");
        line(md, "|---|---:|---:|---:|---:|---:|---:|---:|---:|");
        for (SummaryRow row : summary) {
            if (!row.instance().equals(instance.name())) {
                continue;
            }
            line(md, "| `%s` | %d/%d | %s | %s | %s | %s | %s | %s | %s |", row.solver(), row.succeeded(), row.runs(),
                    text(row.bestProfit()),
                    row.medianGap() == null ? "—" : prefix + number(row.medianGap(), 2) + " %",
                    row.bestGap() == null ? "—" : prefix + number(row.bestGap(), 2) + " %",
                    text(row.optimumHits()), number(row.medianEvaluations(), 0),
                    number(row.medianEvaluationsToBest(), 0), number(row.medianMillis(), 2));
        }
        md.append('\n');

        List<Map<String, Object>> gaps = runs.stream()
                .filter(run -> run.instance().equals(instance.name()) && run.succeeded())
                .map(run -> obj("solver", run.solver(), "gap", run.gap()))
                .toList();
        writeChart(md, directory, "charts/gap-" + chartId + ".json",
                ChartSpecs.gapBoxplot("Distribución del GAP" + (optimum ? "" : " (≤, contra la cota)"), gaps));

        Map<String, List<List<long[]>>> bySolver = new LinkedHashMap<>();
        traces.stream().sorted(Comparator.comparing(TraceRecord::solver))
                .forEach(trace -> bySolver.computeIfAbsent(trace.solver(), key -> new ArrayList<>()).add(trace.points()));
        List<Map<String, Object>> curve = new ArrayList<>();
        bySolver.forEach((solver, solverTraces) -> Convergence.medianCurve(solverTraces, instance.reference())
                .forEach(point -> curve.add(obj("solver", solver, "evaluations", point.evaluations(),
                        "gap", point.gap(), "runs", point.runs()))));
        writeChart(md, directory, "charts/convergencia-" + chartId + ".json",
                ChartSpecs.convergence("Convergencia: GAP mediano del mejor valor encontrado", curve));
    }

    private void writeIndex(List<StoredExperiment> experiments, Path output) throws IOException {
        StringBuilder md = new StringBuilder();
        line(md, "# Resultados");
        line(md, "Cada fila es una ejecución de un experimento. La [evolución](evolucion/index.md) muestra cómo cambian "
                + "los solvers entre ejecuciones del mismo experimento.");
        if (experiments.isEmpty()) {
            line(md, "Todavía no hay resultados publicados.");
        } else {
            line(md, "| Fecha | Experimento | Commit | Instancias | Solvers | Ejecuciones | Fallidas |");
            line(md, "|---|---|---|---:|---:|---:|---:|");
            for (StoredExperiment experiment : experiments) {
                Manifest manifest = experiment.manifest();
                line(md, "| [%s](%s/%s/index.md) | %s | %s | %d | %d | %d | %d |", HUMAN_DATE.format(experiment.startedAt()),
                        experiment.name(), experiment.runId(), experiment.name(), commit(manifest.environment()),
                        manifest.instances().size(), manifest.solvers().size(), manifest.totalRuns(),
                        experiment.runs().stream().filter(run -> !run.succeeded()).count());
            }
        }
        Files.writeString(output.resolve("index.md"), md.toString());
    }

    private void writeEvolution(List<StoredExperiment> experiments, Path directory) throws IOException {
        Files.createDirectories(directory.resolve("charts"));
        StringBuilder md = new StringBuilder();
        line(md, "# Evolución");
        line(md, "Para cada experimento, la media de los GAP medianos por instancia de cada solver en cada ejecución. "
                + "Solo es comparable entre puntos con el mismo **conjunto de instancias** (misma forma del punto): si "
                + "cambian las instancias, cambia la escala. Las instancias en las que un solver no tuvo ninguna "
                + "ejecución correcta no entran en su media.");
        Map<String, List<StoredExperiment>> byName = experiments.stream()
                .collect(Collectors.groupingBy(StoredExperiment::name, java.util.TreeMap::new, Collectors.toList()));
        if (byName.isEmpty()) {
            line(md, "Todavía no hay resultados publicados.");
        }
        for (Map.Entry<String, List<StoredExperiment>> entry : byName.entrySet()) {
            line(md, "## %s", entry.getKey());
            List<Map<String, Object>> rows = new ArrayList<>();
            for (StoredExperiment experiment : entry.getValue()) {
                Comparison comparison = Comparison.of(experiment.runs());
                for (int s = 0; s < comparison.solvers().size(); s++) {
                    double[] finite = finiteColumn(comparison, s);
                    if (finite.length > 0) {
                        rows.add(obj("date", experiment.startedAt().toString(), "solver", comparison.solvers().get(s),
                                "gap", Arrays.stream(finite).average().orElseThrow(), "suite", experiment.suite()));
                    }
                }
            }
            List<StoredExperiment> runs = entry.getValue();
            List<String> changes = new ArrayList<>();
            for (int i = 0; i + 1 < runs.size(); i++) {
                List<String> changed = runs.get(i).codeChangedSince(runs.get(i + 1));
                if (!changed.isEmpty()) {
                    changes.add(String.format(Locale.ROOT, "- **%s**: %s", HUMAN_DATE.format(runs.get(i).startedAt()),
                            changed.stream().map(id -> "`" + id + "`").collect(Collectors.joining(", "))));
                }
            }
            if (!changes.isEmpty()) {
                line(md, "Cambios de código en variantes ya publicadas; sus puntos antes y después no son comparables:");
                changes.forEach(change -> md.append(change).append('\n'));
                md.append('\n');
            }
            if (entry.getValue().size() == 1) {
                line(md, "Solo hay una ejecución de este experimento; la evolución aparecerá con la siguiente.");
            }
            writeChart(md, directory, "charts/" + entry.getKey() + ".json", ChartSpecs.evolution(entry.getKey(), rows));
        }
        Files.writeString(directory.resolve("index.md"), md.toString());
    }

    private String commit(EnvironmentInfo environment) {
        if (environment.gitCommit() == null) {
            return "desconocido";
        }
        String shortSha = environment.gitCommit().substring(0, Math.min(7, environment.gitCommit().length()));
        String link = repositoryUrl == null ? "`" + shortSha + "`"
                : "[`" + shortSha + "`](" + repositoryUrl + "/commit/" + environment.gitCommit() + ")";
        return Boolean.TRUE.equals(environment.gitDirty()) ? link + " (con cambios sin commitear)" : link;
    }

    private static void writeChart(StringBuilder md, Path directory, String relativePath, Map<String, Object> spec)
            throws IOException {
        Files.writeString(directory.resolve(relativePath), JSON.writeValueAsString(spec));
        md.append(String.format(CHART, relativePath));
    }

    private static String meanOfMedianGaps(Comparison comparison, int solver) {
        double[] finite = finiteColumn(comparison, solver);
        int failed = comparison.instances().size() - finite.length;
        String mean = finite.length == 0 ? "—" : number(Arrays.stream(finite).average().orElseThrow(), 2) + " %";
        return failed == 0 ? mean : mean + " (sin ejecuciones correctas en " + failed + ")";
    }

    private static double[] finiteColumn(Comparison comparison, int solver) {
        return Arrays.stream(comparison.medianGaps()).mapToDouble(row -> row[solver]).filter(Double::isFinite).toArray();
    }

    private static void prepareOutput(Path output) throws IOException {
        if (Files.exists(output)) {
            boolean empty;
            try (Stream<Path> entries = Files.list(output)) {
                empty = entries.findAny().isEmpty();
            }
            if (!empty && !Files.exists(output.resolve(MARKER))) {
                throw new IOException(output + " tiene contenido que no generó el subcomando report; no se borra");
            }
            try (Stream<Path> paths = Files.walk(output)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(output);
    }

    private static void line(StringBuilder md, String format, Object... args) {
        md.append(String.format(Locale.ROOT, format, args)).append("\n\n");
        if (format.startsWith("|") || format.startsWith("    |")) {
            md.setLength(md.length() - 1);
        }
    }

    private static void row(StringBuilder md, String label, String value) {
        md.append("| ").append(label).append(" | ").append(value).append(" |\n");
    }

    private static String number(Double value, int decimals) {
        return value == null ? "—" : String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    private static String pValue(double value) {
        return value < 0.001 ? String.format(Locale.ROOT, "%.1e", value) : String.format(Locale.ROOT, "%.3f", value);
    }

    private static String text(Object value) {
        return value == null ? "—" : value.toString();
    }

    private static String escape(String value) {
        return value.replace("|", "\\|");
    }
}
