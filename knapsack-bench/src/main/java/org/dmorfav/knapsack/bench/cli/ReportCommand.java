package org.dmorfav.knapsack.bench.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.dmorfav.knapsack.bench.report.ReportGenerator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Command(name = "report", mixinStandardHelpOptions = true,
        description = "Genera las páginas de resultados de la documentación (MkDocs) a partir de las carpetas de experimentos.")
public final class ReportCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Option(names = "--results", paramLabel = "<carpeta>", defaultValue = "results",
            description = "Carpeta con resultados <experimento>/<marca> (por defecto: ${DEFAULT-VALUE}).")
    Path results;

    @Option(names = "--output", paramLabel = "<carpeta>", defaultValue = "docs/resultados",
            description = "Carpeta generada; se reemplaza entera (por defecto: ${DEFAULT-VALUE}).")
    Path output;

    @Option(names = "--repository", paramLabel = "<url>", defaultValue = "https://github.com/dmorfav/Knapsack",
            description = "URL del repositorio para enlazar commits (por defecto: ${DEFAULT-VALUE}).")
    String repository;

    @Override
    public Integer call() throws IOException {
        PrintWriter out = spec.commandLine().getOut();
        int experiments = new ReportGenerator(repository).generate(results, output);
        out.printf("%d ejecuciones de experimentos publicadas en %s%n", experiments, output);
        out.flush();
        return 0;
    }
}
