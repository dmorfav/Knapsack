package org.dmorfav.knapsack.bench.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dmorfav.knapsack.baselines.DynamicProgrammingSolver;
import org.dmorfav.knapsack.bench.experiment.InstanceSuite;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.generator.GeneratorSpec;
import org.dmorfav.knapsack.core.generator.PisingerGenerator;
import org.dmorfav.knapsack.core.io.InstanceFiles;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "suite", mixinStandardHelpOptions = true,
        description = "Genera en su carpeta las instancias de una suite definida en YAML, todas con su óptimo exacto.")
public final class SuiteCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(paramLabel = "<suite>", description = "Fichero YAML de la suite; las instancias se escriben junto a él.")
    Path suiteFile;

    @Option(names = "--check",
            description = "No escribe: comprueba que la carpeta contiene exactamente la salida del generador.")
    boolean check;

    @Override
    public Integer call() throws IOException {
        PrintWriter out = spec.commandLine().getOut();
        InstanceSuite suite = InstanceSuite.read(suiteFile);
        Path directory = suiteFile.toAbsolutePath().getParent();
        List<GeneratorSpec> specs = suite.specs();
        int problems = 0;

        for (GeneratorSpec generatorSpec : specs) {
            Instance instance = PisingerGenerator.generate(generatorSpec);
            Path target = directory.resolve(generatorSpec.name() + ".json");
            if (!DynamicProgrammingSolver.isTractable(instance)) {
                out.printf(Locale.ROOT, "%s: sin óptimo calculable; la suite exige óptimo en todas sus instancias%n",
                        generatorSpec.name());
                problems++;
                continue;
            }
            instance = instance.withKnownOptimum(instance.evaluate(DynamicProgrammingSolver.optimum(instance)).profit());
            if (!check) {
                InstanceFiles.write(instance, target);
            } else if (!Files.isRegularFile(target)) {
                out.printf(Locale.ROOT, "%s: falta el fichero%n", target.getFileName());
                problems++;
            } else if (!InstanceFiles.read(target).equals(instance)) {
                out.printf(Locale.ROOT, "%s: no coincide con la salida del generador%n", target.getFileName());
                problems++;
            }
        }

        Set<String> expected = specs.stream().map(generatorSpec -> generatorSpec.name() + ".json")
                .collect(Collectors.toCollection(TreeSet::new));
        try (Stream<Path> files = Files.list(directory)) {
            for (Path extra : files.filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> !expected.contains(path.getFileName().toString())).sorted().toList()) {
                out.printf(Locale.ROOT, "%s: sobra, no pertenece a la suite%n", extra.getFileName());
                problems++;
            }
        }

        out.printf(Locale.ROOT, "Suite %s: %d instancias %s en %s%s%n", suite.name(), specs.size(),
                check ? "comprobadas" : "generadas", directory, problems == 0 ? "" : ", " + problems + " problemas");
        out.flush();
        return problems == 0 ? 0 : 1;
    }
}
