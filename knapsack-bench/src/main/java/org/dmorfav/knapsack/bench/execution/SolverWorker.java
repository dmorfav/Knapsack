package org.dmorfav.knapsack.bench.execution;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.dmorfav.knapsack.bench.SolverRun;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;

/** Lado del proceso aislado: atiende peticiones de un único solver hasta que se cierra la entrada. */
public final class SolverWorker {

    private SolverWorker() {
    }

    public static void serve(KnapsackSolver solver, InputStream requests, PrintStream responses) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(requests, StandardCharsets.UTF_8));
        Map<Integer, Instance> instances = new HashMap<>();
        responses.println(WorkerProtocol.READY);
        responses.flush();
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            WorkerProtocol.Request request = WorkerProtocol.MAPPER.readValue(line, WorkerProtocol.Request.class);
            if (request.instance() != null) {
                instances.put(request.instanceId(), request.instance().toInstance());
            }
            Instance instance = instances.get(request.instanceId());
            if (instance == null) {
                throw new IOException("petición con la instancia " + request.instanceId() + ", que no se ha enviado");
            }
            SolverRun.Attempt attempt = SolverRun.attempt(solver, instance, request.budget(), request.seed());
            responses.println(WorkerProtocol.MAPPER.writeValueAsString(WorkerProtocol.Response.from(attempt)));
            responses.flush();
        }
    }

    /** El mismo requisito que ServiceLoader: clase pública con constructor público sin argumentos. */
    public static KnapsackSolver instantiate(String className) {
        try {
            return Class.forName(className).asSubclass(KnapsackSolver.class).getConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            Throwable cause = e instanceof InvocationTargetException invocation ? invocation.getCause() : e;
            throw new IllegalArgumentException("no se puede instanciar el solver " + className + ": " + cause, cause);
        }
    }
}
