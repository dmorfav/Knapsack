package org.dmorfav.knapsack.bench.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import org.dmorfav.knapsack.bench.experiment.Manifest;
import org.dmorfav.knapsack.bench.experiment.ResultsDirectory;
import org.dmorfav.knapsack.bench.experiment.RunRecord;

/** Un experimento ya ejecutado, leído de su carpeta de resultados. */
record StoredExperiment(ResultsDirectory directory, Manifest manifest, List<RunRecord> runs) {

    static StoredExperiment load(Path path) throws IOException {
        ResultsDirectory directory = new ResultsDirectory(path);
        return new StoredExperiment(directory, directory.readManifest(), directory.readRuns());
    }

    /** Carpetas {@code <experimento>/<marca>} con manifest.json y runs.jsonl, de la más reciente a la más antigua. */
    static List<StoredExperiment> discover(Path resultsRoot) throws IOException {
        if (!Files.isDirectory(resultsRoot)) {
            return List.of();
        }
        List<StoredExperiment> experiments = new ArrayList<>();
        try (Stream<Path> candidates = Files.find(resultsRoot, 2, (path, attributes) -> attributes.isDirectory()
                && Files.isRegularFile(path.resolve(ResultsDirectory.MANIFEST))
                && Files.isRegularFile(path.resolve(ResultsDirectory.RUNS)))) {
            for (Path candidate : candidates.toList()) {
                experiments.add(load(candidate));
            }
        }
        experiments.sort(Comparator.comparing(StoredExperiment::startedAt).reversed()
                .thenComparing(experiment -> experiment.directory().path().toString()));
        return experiments;
    }

    String name() {
        return manifest.experiment();
    }

    String runId() {
        return directory.path().getFileName().toString();
    }

    Instant startedAt() {
        return Instant.parse(manifest.startedAt());
    }

    /** Huella corta del conjunto de instancias: solo es comparable la evolución entre ejecuciones con la misma. */
    String suite() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            manifest.instances().stream().map(Manifest.InstanceEntry::sha256).sorted()
                    .forEach(hash -> digest.update(hash.getBytes(StandardCharsets.UTF_8)));
            return HexFormat.of().formatHex(digest.digest()).substring(0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("la JVM no ofrece SHA-256", e);
        }
    }
}
