package org.dmorfav.knapsack.bench.execution;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.dmorfav.knapsack.bench.Main;
import org.dmorfav.knapsack.bench.SolverRun;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import tools.jackson.core.JacksonException;

/**
 * Ejecuta cada solver en su propia JVM, que se reutiliza entre ejecuciones para conservar el calentamiento del JIT.
 * Si una ejecución supera el presupuesto de tiempo más un margen, o el proceso muere, se destruye el proceso, la
 * ejecución cuenta como fallida y el siguiente uso arranca uno nuevo. Tras {@code maxProcessFailures} fallos de
 * proceso, el solver queda descalificado y el resto de sus ejecuciones fallan sin lanzarse.
 */
public final class IsolatedExecutor implements SolverExecutor {

    public static final Duration DEFAULT_GRACE = Duration.ofSeconds(5);
    static final int DEFAULT_MAX_PROCESS_FAILURES = 3;
    /** Tiempo para arrancar la JVM y construir el solver, aparte del límite duro de cada ejecución. */
    static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(60);

    private final List<String> launcher;
    private final Duration grace;
    private final int maxProcessFailures;
    private final Map<String, WorkerProcess> processes = new HashMap<>();
    private final Map<String, Integer> processFailures = new HashMap<>();

    IsolatedExecutor(List<String> launcher, Duration grace, int maxProcessFailures) {
        this.launcher = List.copyOf(launcher);
        this.grace = grace;
        this.maxProcessFailures = maxProcessFailures;
    }

    /** Lanza los procesos con el mismo JDK y classpath que esta JVM. */
    public static IsolatedExecutor forCurrentJvm(Duration grace) {
        return forCurrentJvm(grace, DEFAULT_MAX_PROCESS_FAILURES);
    }

    static IsolatedExecutor forCurrentJvm(Duration grace, int maxProcessFailures) {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        return new IsolatedExecutor(List.of(java, "-cp", currentClasspath(), Main.class.getName(), "worker"),
                grace, maxProcessFailures);
    }

    @Override
    public SolverRun execute(KnapsackSolver solver, Instance instance, Budget budget, long seed) {
        String solverClass = isolatableClassName(solver);
        if (processFailures.getOrDefault(solverClass, 0) >= maxProcessFailures) {
            return SolverRun.of(solver.info(), seed, instance, SolverRun.Attempt.failed(
                    "descalificado: su proceso falló " + maxProcessFailures + " veces en este experimento", 0, Duration.ZERO));
        }
        long startNanos = System.nanoTime();
        try {
            WorkerProcess process = processes.get(solverClass);
            if (process == null) {
                process = start(solverClass);
                processes.put(solverClass, process);
                startNanos = System.nanoTime();
            }
            return SolverRun.of(solver.info(), seed, instance, process.run(instance, budget, seed, hardLimit(budget)));
        } catch (ProcessFailure e) {
            WorkerProcess failed = processes.remove(solverClass);
            if (failed != null) {
                failed.destroy();
            }
            processFailures.merge(solverClass, 1, Integer::sum);
            return SolverRun.of(solver.info(), seed, instance,
                    SolverRun.Attempt.failed(e.getMessage(), 0, Duration.ofNanos(System.nanoTime() - startNanos)));
        }
    }

    @Override
    public String description() {
        return "un proceso por solver; límite duro: presupuesto de tiempo + " + describe(grace);
    }

    @Override
    public void close() {
        processes.values().forEach(WorkerProcess::close);
        processes.clear();
    }

    private Duration hardLimit(Budget budget) {
        return budget.limitsTime() ? budget.maxTime().plus(grace) : null;
    }

    /** Lanza el proceso y espera a que construya el solver: el arranque en frío de la JVM no cuenta en el límite duro. */
    private WorkerProcess start(String solverClass) throws ProcessFailure {
        List<String> command = new ArrayList<>(launcher);
        command.add("--solver-class=" + solverClass);
        WorkerProcess worker;
        try {
            worker = new WorkerProcess(new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start());
        } catch (IOException e) {
            throw new UncheckedIOException("no se pudo lanzar el proceso de " + solverClass, e);
        }
        try {
            worker.awaitReady(STARTUP_TIMEOUT);
            return worker;
        } catch (ProcessFailure e) {
            worker.destroy();
            throw e;
        }
    }

    private static String isolatableClassName(KnapsackSolver solver) {
        Class<?> type = solver.getClass();
        try {
            if (Modifier.isPublic(type.getModifiers()) && !type.isAnonymousClass() && !type.isHidden()
                    && Modifier.isPublic(type.getConstructor().getModifiers())) {
                return type.getName();
            }
        } catch (NoSuchMethodException e) {
            // cae al error de abajo
        }
        throw new IllegalArgumentException("el solver " + solver.info().id() + " (" + type.getName()
                + ") necesita ser una clase pública con constructor público sin argumentos para ejecutarse aislado");
    }

    static String currentClasspath() {
        for (ClassLoader loader = IsolatedExecutor.class.getClassLoader(); loader != null; loader = loader.getParent()) {
            // exec:java carga el proyecto en un URLClassLoader: java.class.path sería el de Maven
            if (loader instanceof URLClassLoader urls && urls.getURLs().length > 0) {
                List<String> entries = new ArrayList<>();
                for (URL url : urls.getURLs()) {
                    try {
                        entries.add(Path.of(url.toURI()).toString());
                    } catch (URISyntaxException | IllegalArgumentException e) {
                        throw new IllegalStateException("entrada de classpath no convertible a ruta: " + url, e);
                    }
                }
                return String.join(File.pathSeparator, entries);
            }
        }
        return System.getProperty("java.class.path");
    }

    private static String describe(Duration duration) {
        return duration.toMillis() % 1000 == 0 ? duration.toSeconds() + " s" : duration.toMillis() + " ms";
    }

    private static final class ProcessFailure extends Exception {
        ProcessFailure(String message) {
            super(message);
        }
    }

    private static final class WorkerProcess {

        private final Process process;
        private final BufferedWriter requests;
        private final BlockingQueue<Optional<String>> responses = new LinkedBlockingQueue<>();
        private final Map<Instance, Integer> sentInstances = new IdentityHashMap<>();

        WorkerProcess(Process process) {
            this.process = process;
            this.requests = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            Thread.ofPlatform().daemon().name("worker-reader-" + process.pid()).start(this::readResponses);
        }

        void awaitReady(Duration timeout) throws ProcessFailure {
            Optional<String> line = awaitResponse(timeout);
            if (line == null) {
                throw new ProcessFailure("su proceso no arrancó en " + describe(timeout));
            }
            if (line.isEmpty()) {
                throw new ProcessFailure(crashMessage());
            }
            if (!line.get().equals(WorkerProtocol.READY)) {
                throw new ProcessFailure("su proceso respondió algo inesperado al arrancar: " + line.get());
            }
        }

        SolverRun.Attempt run(Instance instance, Budget budget, long seed, Duration hardLimit) throws ProcessFailure {
            Integer known = sentInstances.get(instance);
            int instanceId = known != null ? known : sentInstances.size();
            WorkerProtocol.Request request = WorkerProtocol.Request.of(instanceId, known != null ? null : instance, budget, seed);
            try {
                requests.write(WorkerProtocol.MAPPER.writeValueAsString(request));
                requests.newLine();
                requests.flush();
            } catch (IOException e) {
                throw new ProcessFailure(crashMessage());
            }
            sentInstances.put(instance, instanceId);

            Optional<String> line = awaitResponse(hardLimit);
            if (line == null) {
                throw new ProcessFailure(String.format(Locale.ROOT,
                        "no terminó en el límite duro de %s (presupuesto de tiempo + margen): se detuvo su proceso",
                        describe(hardLimit)));
            }
            if (line.isEmpty()) {
                throw new ProcessFailure(crashMessage());
            }
            try {
                return WorkerProtocol.MAPPER.readValue(line.get(), WorkerProtocol.Response.class).toAttempt();
            } catch (JacksonException e) {
                throw new ProcessFailure("su proceso respondió algo que no es una respuesta válida: " + e.getOriginalMessage());
            }
        }

        void destroy() {
            process.destroyForcibly();
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        void close() {
            try {
                requests.close();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    destroy();
                }
            } catch (IOException e) {
                destroy();
            } catch (InterruptedException e) {
                destroy();
                Thread.currentThread().interrupt();
            }
        }

        /** null si se agotó el límite; vacío si el proceso cerró su salida. */
        private Optional<String> awaitResponse(Duration hardLimit) throws ProcessFailure {
            try {
                return hardLimit == null
                        ? responses.take()
                        : responses.poll(hardLimit.toNanos(), TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProcessFailure("interrumpido mientras esperaba al proceso");
            }
        }

        private String crashMessage() {
            try {
                if (process.waitFor(5, TimeUnit.SECONDS)) {
                    return "su proceso terminó inesperadamente con código " + process.exitValue();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "su proceso dejó de responder";
        }

        private void readResponses() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    responses.add(Optional.of(line));
                }
            } catch (IOException e) {
                // el proceso se destruyó: se trata como fin de la salida
            }
            responses.add(Optional.empty());
        }
    }
}
