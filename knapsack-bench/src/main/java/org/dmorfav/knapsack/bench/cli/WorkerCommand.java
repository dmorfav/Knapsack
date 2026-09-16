package org.dmorfav.knapsack.bench.cli;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import org.dmorfav.knapsack.bench.execution.SolverWorker;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "worker", hidden = true,
        description = "Uso interno del ejecutor aislado: atiende por la entrada estándar las ejecuciones de un solver.")
public final class WorkerCommand implements Callable<Integer> {

    @Option(names = "--solver-class", required = true, paramLabel = "<clase>")
    String solverClass;

    @Override
    public Integer call() throws IOException {
        // La salida estándar es el canal del protocolo: lo que imprima el solver va a la salida de errores
        PrintStream protocol = new PrintStream(new FileOutputStream(FileDescriptor.out), false, StandardCharsets.UTF_8);
        System.setOut(System.err);
        SolverWorker.serve(SolverWorker.instantiate(solverClass), new FileInputStream(FileDescriptor.in), protocol);
        return 0;
    }
}
