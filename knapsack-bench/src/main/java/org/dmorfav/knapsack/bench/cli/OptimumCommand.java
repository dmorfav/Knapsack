package org.dmorfav.knapsack.bench.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.dmorfav.knapsack.baselines.DynamicProgrammingSolver;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.io.InstanceFiles;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "optimum", mixinStandardHelpOptions = true,
        description = "Calcula el óptimo exacto de instancias y lo contrasta con el que declaran.")
public final class OptimumCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(arity = "1..*", paramLabel = "<instancia>", description = "Ficheros de instancia JSON.")
    List<Path> instances;

    @Option(names = "--write", description = "Guarda el óptimo en las instancias que no lo declaran.")
    boolean write;

    @Override
    public Integer call() throws IOException {
        PrintWriter out = spec.commandLine().getOut();
        int exitCode = 0;
        for (Path path : instances) {
            Instance instance = InstanceFiles.read(path);
            if (!DynamicProgrammingSolver.isTractable(instance)) {
                out.printf(Locale.ROOT, "%s: no calculable, n·(C+1) supera el límite de la programación dinámica%n", path);
                exitCode = 1;
                continue;
            }
            long optimum = instance.evaluate(DynamicProgrammingSolver.optimum(instance)).profit();
            if (instance.knownOptimum().isPresent()) {
                long declared = instance.knownOptimum().getAsLong();
                if (declared == optimum) {
                    out.printf(Locale.ROOT, "%s: óptimo %d, coincide con el declarado%n", path, optimum);
                } else {
                    out.printf(Locale.ROOT, "%s: DISCREPANCIA, declara %d y el óptimo es %d%n", path, declared, optimum);
                    exitCode = 1;
                }
            } else if (write) {
                InstanceFiles.write(instance.withKnownOptimum(optimum), path);
                out.printf(Locale.ROOT, "%s: óptimo %d, guardado%n", path, optimum);
            } else {
                out.printf(Locale.ROOT, "%s: óptimo %d (usa --write para guardarlo)%n", path, optimum);
            }
        }
        out.flush();
        return exitCode;
    }
}
