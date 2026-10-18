package org.dmorfav.knapsack.bench.execution;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.dmorfav.knapsack.bench.SolverRun;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.TracePoint;
import tools.jackson.databind.json.JsonMapper;

/**
 * Mensajes entre el banco y un proceso aislado: una línea JSON por petición y por respuesta. El proceso anuncia
 * {@link #READY} cuando ha construido el solver, antes de la primera petición. Cada instancia se envía una sola vez
 * por proceso; las peticiones siguientes la citan por su id.
 */
final class WorkerProtocol {

    static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** Primera línea del proceso: a partir de aquí el límite duro solo mide ejecuciones, no el arranque de la JVM. */
    static final String READY = "{\"ready\":true}";

    private WorkerProtocol() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Request(int instanceId, InstancePayload instance, long seed, long maxEvaluations, long maxTimeNanos) {

        static Request of(int instanceId, Instance instance, Budget budget, long seed) {
            return new Request(instanceId, instance == null ? null : InstancePayload.from(instance), seed,
                    budget.maxEvaluations(), budget.maxTime().toNanos());
        }

        Budget budget() {
            return new Budget(maxEvaluations, Duration.ofNanos(maxTimeNanos));
        }
    }

    record InstancePayload(String name, long capacity, long[] profits, long[] weights) {

        static InstancePayload from(Instance instance) {
            long[] profits = new long[instance.size()];
            long[] weights = new long[instance.size()];
            for (int i = 0; i < instance.size(); i++) {
                profits[i] = instance.item(i).profit();
                weights[i] = instance.item(i).weight();
            }
            return new InstancePayload(instance.name(), instance.capacity(), profits, weights);
        }

        Instance toInstance() {
            List<Item> items = new ArrayList<>(profits.length);
            for (int i = 0; i < profits.length; i++) {
                items.add(new Item(profits[i], weights[i]));
            }
            return new Instance(name, capacity, items);
        }
    }

    /** {@code solution} va como cadena de bits, que conserva el tamaño; {@code trace} como [evaluación, nanos, beneficio]. */
    record Response(String solution, long evaluations, long elapsedNanos, List<long[]> trace, String failure) {

        static Response from(SolverRun.Attempt attempt) {
            List<long[]> trace = attempt.trace().stream()
                    .map(point -> new long[] {point.evaluation(), point.elapsedNanos(), point.profit()})
                    .toList();
            String solution = attempt.solution() == null ? null : attempt.solution().toString();
            return new Response(solution, attempt.evaluations(), attempt.elapsed().toNanos(), trace, attempt.failure());
        }

        SolverRun.Attempt toAttempt() {
            List<TracePoint> points = trace.stream().map(point -> new TracePoint(point[0], point[1], point[2])).toList();
            return new SolverRun.Attempt(solution == null ? null : parseBits(solution), evaluations,
                    Duration.ofNanos(elapsedNanos), points, failure);
        }

        private static Solution parseBits(String bits) {
            boolean[] selected = new boolean[bits.length()];
            for (int i = 0; i < bits.length(); i++) {
                selected[i] = bits.charAt(i) == '1';
            }
            return Solution.of(selected);
        }
    }
}
