package org.dmorfav.knapsack.bench.experiment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.dmorfav.knapsack.bench.Reference;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.io.InstanceFiles;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;

/** Experimento con patrones resueltos: ficheros concretos, solvers concretos y orden de ejecución. */
public record ExperimentPlan(Experiment experiment, List<PlannedInstance> instances, List<KnapsackSolver> solvers,
                             List<Task> tasks) {

    public record PlannedInstance(String file, String sha256, Instance instance, Reference reference) {
    }

    public record Task(PlannedInstance instance, KnapsackSolver solver, long seed) {
    }

    /** Resuelve patrones relativos a {@code root}. El orden de las tareas se baraja con firstSeed: reproducible. */
    public static ExperimentPlan resolve(Experiment experiment, Path root, List<KnapsackSolver> available) throws IOException {
        List<PlannedInstance> instances = new ArrayList<>();
        for (String file : resolveInstanceFiles(root, experiment.instances())) {
            Path path = root.resolve(file);
            Instance instance = InstanceFiles.read(path);
            instances.add(new PlannedInstance(file, sha256(path), instance, Reference.of(instance)));
        }
        List<KnapsackSolver> solvers = selectSolvers(available, experiment.solvers());

        List<Task> tasks = new ArrayList<>();
        for (PlannedInstance instance : instances) {
            for (KnapsackSolver solver : solvers) {
                for (int run = 0; run < experiment.runs(); run++) {
                    tasks.add(new Task(instance, solver, experiment.seed(run)));
                }
            }
        }
        Collections.shuffle(tasks, new Random(experiment.firstSeed()));
        return new ExperimentPlan(experiment, List.copyOf(instances), solvers, List.copyOf(tasks));
    }

    static List<String> resolveInstanceFiles(Path root, List<String> patterns) throws IOException {
        Set<String> files = new LinkedHashSet<>();
        for (String pattern : patterns) {
            String normalized = pattern.replace('\\', '/');
            List<String> matches = Glob.hasWildcards(normalized)
                    ? expand(root, normalized)
                    : Files.isRegularFile(root.resolve(normalized)) ? List.of(normalized) : List.of();
            if (matches.isEmpty()) {
                throw new IOException("el patrón de instancias '" + pattern + "' no encuentra ningún fichero");
            }
            files.addAll(matches);
        }
        return List.copyOf(files);
    }

    static List<KnapsackSolver> selectSolvers(List<KnapsackSolver> available, List<String> patterns) {
        List<Pattern> compiled = patterns.stream().map(Glob::forIds).toList();
        for (int i = 0; i < patterns.size(); i++) {
            Pattern pattern = compiled.get(i);
            if (available.stream().noneMatch(solver -> pattern.matcher(solver.info().id()).matches())) {
                throw new IllegalArgumentException("el patrón de solvers '" + patterns.get(i) + "' no selecciona ninguno");
            }
        }
        return available.stream()
                .filter(solver -> compiled.stream().anyMatch(pattern -> pattern.matcher(solver.info().id()).matches()))
                .toList();
    }

    private static List<String> expand(Path root, String pattern) throws IOException {
        String[] segments = pattern.split("/");
        StringBuilder base = new StringBuilder();
        for (String segment : segments) {
            if (Glob.hasWildcards(segment)) {
                break;
            }
            base.append(segment).append('/');
        }
        Path start = root.resolve(base.toString());
        if (!Files.isDirectory(start)) {
            return List.of();
        }
        Pattern regex = Glob.forPaths(pattern);
        try (Stream<Path> paths = Files.walk(start)) {
            return paths.filter(Files::isRegularFile)
                    .map(path -> root.relativize(path).toString().replace('\\', '/'))
                    .filter(relative -> regex.matcher(relative).matches())
                    .sorted()
                    .toList();
        }
    }

    private static String sha256(Path path) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("la JVM no ofrece SHA-256", e);
        }
    }
}
