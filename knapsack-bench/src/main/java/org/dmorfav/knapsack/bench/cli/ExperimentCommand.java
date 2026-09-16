package org.dmorfav.knapsack.bench.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.Callable;

import org.dmorfav.knapsack.bench.SolverCatalog;
import org.dmorfav.knapsack.bench.experiment.Experiment;
import org.dmorfav.knapsack.bench.experiment.ExperimentRunner;
import org.dmorfav.knapsack.bench.experiment.ResultsDirectory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "experiment", mixinStandardHelpOptions = true,
        description = "Ejecuta un experimento definido en YAML y guarda manifest.json, runs.jsonl, traces.jsonl y summary.csv.")
public final class ExperimentCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(paramLabel = "<experimento>", description = "Fichero YAML del experimento.")
    Path experimentFile;

    @Option(names = "--output", paramLabel = "<carpeta>", defaultValue = "results",
            description = "Carpeta base de resultados (por defecto: ${DEFAULT-VALUE}).")
    Path output;

    @Override
    public Integer call() throws IOException {
        PrintWriter out = spec.commandLine().getOut();
        Experiment experiment = Experiment.read(experimentFile);
        ResultsDirectory results = new ExperimentRunner(SolverCatalog.discover(), out, Clock.systemUTC())
                .run(experiment, Path.of("").toAbsolutePath(), output);
        out.printf("%nResultados: %s%n", results.path());
        out.flush();
        return 0;
    }
}
