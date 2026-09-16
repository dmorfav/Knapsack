package org.dmorfav.knapsack.bench.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import org.dmorfav.knapsack.bench.SolverRun;
import org.dmorfav.knapsack.baselines.DynamicProgrammingSolver;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;
import org.junit.jupiter.api.Test;

class SolverWorkerTest {

    private final Instance instance = new Instance("trio", 10, List.of(
            new Item(6, 5), new Item(5, 5), new Item(9, 8)));

    @Test
    void servesRunsAndSendsEachInstanceOnlyOnce() throws IOException {
        Budget budget = Budget.ofTime(Duration.ofSeconds(5));
        String requests = request(0, instance, budget, 1) + request(0, null, budget, 2);

        List<WorkerProtocol.Response> responses = serve(new DynamicProgrammingSolver(), requests);

        assertThat(responses).hasSize(2).allSatisfy(response -> {
            assertThat(response.solution()).isEqualTo("110");
            assertThat(response.failure()).isNull();
            assertThat(response.evaluations()).isEqualTo(1);
            assertThat(response.trace()).hasSize(1);
        });
    }

    @Test
    void responsesRoundTripToTheSameAttempt() {
        SolverRun.Attempt attempt = SolverRun.attempt(new DynamicProgrammingSolver(), instance, Budget.unlimited(), 1);

        SolverRun.Attempt decoded = WorkerProtocol.MAPPER.readValue(
                WorkerProtocol.MAPPER.writeValueAsString(WorkerProtocol.Response.from(attempt)),
                WorkerProtocol.Response.class).toAttempt();

        assertThat(decoded.solution()).isEqualTo(Solution.fromBits(1, 1, 0));
        assertThat(decoded.evaluations()).isEqualTo(attempt.evaluations());
        assertThat(decoded.elapsed()).isEqualTo(attempt.elapsed());
        assertThat(decoded.trace()).isEqualTo(attempt.trace());
        assertThat(decoded.failure()).isNull();
    }

    @Test
    void failuresTravelWithoutSolution() throws IOException {
        String requests = request(0, instance, Budget.unlimited(), 1);

        List<WorkerProtocol.Response> responses = serve(new ThrowingSolver(), requests);

        assertThat(responses).singleElement().satisfies(response -> {
            assertThat(response.solution()).isNull();
            assertThat(response.failure()).isEqualTo("excepción IllegalStateException: roto");
        });
    }

    @Test
    void rejectsAnInstanceThatWasNeverSent() {
        String requests = request(7, null, Budget.unlimited(), 1);

        assertThatThrownBy(() -> serve(new DynamicProgrammingSolver(), requests))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("instancia 7");
    }

    @Test
    void instantiatesOnlyPublicSolversWithANoArgumentConstructor() {
        assertThat(SolverWorker.instantiate(NoisySolver.class.getName())).isInstanceOf(NoisySolver.class);
        assertThatThrownBy(() -> SolverWorker.instantiate(String.class.getName()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SolverWorker.instantiate("no.existe.Solver"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no.existe.Solver");
    }

    private static String request(int id, Instance instance, Budget budget, long seed) {
        return WorkerProtocol.MAPPER.writeValueAsString(WorkerProtocol.Request.of(id, instance, budget, seed)) + "\n";
    }

    private static List<WorkerProtocol.Response> serve(KnapsackSolver solver,
                                                       String requests) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        SolverWorker.serve(solver, new ByteArrayInputStream(requests.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8));
        return output.toString(StandardCharsets.UTF_8).lines()
                .map(line -> WorkerProtocol.MAPPER.readValue(line, WorkerProtocol.Response.class))
                .toList();
    }

    public static final class ThrowingSolver implements KnapsackSolver {

        @Override
        public SolverInfo info() {
            return new SolverInfo("test", "throwing", "Test");
        }

        @Override
        public Solution solve(Instance instance, SolverContext context) {
            throw new IllegalStateException("roto");
        }
    }
}
