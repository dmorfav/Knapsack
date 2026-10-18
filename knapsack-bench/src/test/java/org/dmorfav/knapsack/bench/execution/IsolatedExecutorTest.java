package org.dmorfav.knapsack.bench.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

/** Lanza JVMs reales: es la única prueba de que el classpath, el protocolo y el límite duro funcionan juntos. */
class IsolatedExecutorTest {

    private static final Duration GRACE = Duration.ofSeconds(1);
    private static final Budget SHORT = Budget.ofTime(Duration.ofMillis(300));

    private final Instance instance = new Instance("trio", 10, List.of(
            new Item(6, 5), new Item(5, 5), new Item(9, 8)));

    @Test
    void matchesAnInProcessRun() {
        Budget budget = Budget.ofTime(Duration.ofSeconds(10));
        SolverRun inProcess = SolverRun.execute(new DynamicProgrammingSolver(), instance, budget, 4);

        try (IsolatedExecutor executor = IsolatedExecutor.forCurrentJvm(GRACE)) {
            SolverRun first = executor.execute(new DynamicProgrammingSolver(), instance, budget, 4);
            SolverRun reused = executor.execute(new DynamicProgrammingSolver(), instance, budget, 5);

            for (SolverRun isolated : List.of(first, reused)) {
                assertThat(isolated.succeeded()).isTrue();
                assertThat(isolated.solution()).isEqualTo(inProcess.solution());
                assertThat(isolated.evaluation()).isEqualTo(inProcess.evaluation());
                assertThat(isolated.evaluations()).isEqualTo(inProcess.evaluations());
                assertThat(isolated.trace()).hasSameSizeAs(inProcess.trace());
            }
            assertThat(reused.seed()).isEqualTo(5);
        }
    }

    @Test
    void solverOutputDoesNotCorruptTheProtocol() {
        try (IsolatedExecutor executor = IsolatedExecutor.forCurrentJvm(GRACE)) {
            SolverRun run = executor.execute(new NoisySolver(), instance, SHORT, 1);

            assertThat(run.succeeded()).isTrue();
            assertThat(run.solution()).isEqualTo(Solution.empty(3));
        }
    }

    @Test
    void killsARunThatExceedsTheHardLimitAndRecovers() {
        try (IsolatedExecutor executor = IsolatedExecutor.forCurrentJvm(GRACE)) {
            SolverRun hung = executor.execute(new HangingSolver(), instance, SHORT, 1);

            assertThat(hung.succeeded()).isFalse();
            assertThat(hung.failure()).contains("límite duro de 1300 ms");
            assertThat(hung.elapsed()).isGreaterThanOrEqualTo(Duration.ofMillis(1300));

            assertThat(executor.execute(new NoisySolver(), instance, SHORT, 1).succeeded()).isTrue();
        }
    }

    @Test
    void theHardLimitDoesNotCountTheStartupOfTheProcess() {
        try (IsolatedExecutor executor = IsolatedExecutor.forCurrentJvm(GRACE)) {
            SolverRun run = executor.execute(new SlowStartingSolver(), instance, SHORT, 1);

            assertThat(run.failure()).isNull();
            assertThat(run.elapsed()).isLessThan(Duration.ofMillis(SlowStartingSolver.STARTUP_MILLIS));
        } catch (InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void reportsAProcessThatFailsToStart() {
        try (IsolatedExecutor executor = IsolatedExecutor.forCurrentJvm(GRACE)) {
            SolverRun run = executor.execute(new FailingConstructorSolver(), instance, SHORT, 1);

            assertThat(run.succeeded()).isFalse();
            assertThat(run.failure()).contains("terminó inesperadamente");
        }
    }

    @Test
    void reportsAProcessThatDiesAndDisqualifiesItAfterRepeatedFailures() {
        try (IsolatedExecutor executor = IsolatedExecutor.forCurrentJvm(GRACE, 2)) {
            assertThat(executor.execute(new ExitingSolver(), instance, SHORT, 1).failure()).contains("código 3");
            assertThat(executor.execute(new ExitingSolver(), instance, SHORT, 2).failure()).contains("código 3");

            SolverRun disqualified = executor.execute(new ExitingSolver(), instance, SHORT, 3);

            assertThat(disqualified.failure()).startsWith("descalificado");
            assertThat(disqualified.elapsed()).isZero();
        }
    }

    @Test
    void refusesSolversThatCannotBeInstantiatedInAnotherProcess() {
        KnapsackSolver anonymous = new KnapsackSolver() {
            @Override
            public SolverInfo info() {
                return new SolverInfo("test", "anonymous", "Test");
            }

            @Override
            public Solution solve(Instance instance, SolverContext context) {
                return Solution.empty(instance.size());
            }
        };

        try (IsolatedExecutor executor = IsolatedExecutor.forCurrentJvm(GRACE)) {
            assertThatThrownBy(() -> executor.execute(anonymous, instance, SHORT, 1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("test/anonymous");
        }
    }
}
