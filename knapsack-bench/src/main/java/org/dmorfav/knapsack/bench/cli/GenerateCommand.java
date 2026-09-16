package org.dmorfav.knapsack.bench.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.dmorfav.knapsack.baselines.DynamicProgrammingSolver;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.generator.GeneratorSpec;
import org.dmorfav.knapsack.core.generator.InstanceClass;
import org.dmorfav.knapsack.core.generator.PisingerGenerator;
import org.dmorfav.knapsack.core.io.InstanceFiles;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "generate", mixinStandardHelpOptions = true,
        description = "Genera una instancia con las distribuciones de Pisinger y calcula su óptimo si es viable.")
public final class GenerateCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = "--class", required = true, paramLabel = "<clase>", converter = InstanceClassConverter.class,
            completionCandidates = InstanceClassSlugs.class, description = "Clase de instancia: ${COMPLETION-CANDIDATES}.")
    InstanceClass type;

    @Option(names = "--items", defaultValue = "100", description = "Número de objetos (por defecto: ${DEFAULT-VALUE}).")
    int items;

    @Option(names = "--range", defaultValue = "1000", description = "Rango R de coeficientes (por defecto: ${DEFAULT-VALUE}).")
    long range;

    @Option(names = "--instance-number", defaultValue = "50",
            description = "h: posición en la serie, fija la capacidad (por defecto: ${DEFAULT-VALUE}).")
    int instanceNumber;

    @Option(names = "--series", defaultValue = "100", description = "H: tamaño de la serie (por defecto: ${DEFAULT-VALUE}).")
    int seriesSize;

    @Option(names = "--seed", defaultValue = "1", description = "Semilla del generador (por defecto: ${DEFAULT-VALUE}).")
    long seed;

    @Option(names = "--output", paramLabel = "<ruta>",
            description = "Fichero de salida (por defecto instances/pisinger/<nombre>.json).")
    Path output;

    @Option(names = "--no-optimum", description = "No calcula el óptimo.")
    boolean skipOptimum;

    @Override
    public Integer call() throws IOException {
        PrintWriter out = spec.commandLine().getOut();
        GeneratorSpec generatorSpec = new GeneratorSpec(type, items, range, instanceNumber, seriesSize, seed);
        Instance instance = PisingerGenerator.generate(generatorSpec);
        Path target = output != null ? output : Path.of("instances", "pisinger", generatorSpec.name() + ".json");

        String optimumNote;
        if (skipOptimum) {
            optimumNote = "sin óptimo (--no-optimum)";
        } else if (!DynamicProgrammingSolver.isTractable(instance)) {
            optimumNote = "sin óptimo: n·(C+1) supera el límite de la programación dinámica";
        } else {
            long start = System.nanoTime();
            long optimum = instance.evaluate(DynamicProgrammingSolver.optimum(instance)).profit();
            instance = instance.withKnownOptimum(optimum);
            optimumNote = String.format(Locale.ROOT, "óptimo %d (%.0f ms)", optimum, (System.nanoTime() - start) / 1e6);
        }

        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }
        InstanceFiles.write(instance, target);
        out.printf(Locale.ROOT, "%s: %d objetos, capacidad %d, %s%n", target, instance.size(), instance.capacity(), optimumNote);
        out.flush();
        return 0;
    }

    static final class InstanceClassConverter implements ITypeConverter<InstanceClass> {
        @Override
        public InstanceClass convert(String value) {
            return InstanceClass.fromSlug(value);
        }
    }

    static final class InstanceClassSlugs implements Iterable<String> {
        @Override
        public Iterator<String> iterator() {
            return Arrays.stream(InstanceClass.values()).map(InstanceClass::slug).iterator();
        }
    }
}
