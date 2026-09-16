package org.dmorfav.knapsack.bench.execution;

import org.dmorfav.knapsack.bench.SolverRun;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;

/** Dónde y cómo se ejecuta cada llamada a un solver. */
public interface SolverExecutor extends AutoCloseable {

    SolverRun execute(KnapsackSolver solver, Instance instance, Budget budget, long seed);

    /** Texto para el manifiesto y la salida: cómo se aislaron las ejecuciones. */
    String description();

    @Override
    void close();

    /** Aislado por defecto; en proceso solo para depurar, porque un solver que no termina bloquea la JVM. */
    static SolverExecutor create(boolean inProcess) {
        return inProcess ? new InProcessExecutor() : IsolatedExecutor.forCurrentJvm(IsolatedExecutor.DEFAULT_GRACE);
    }

    final class InProcessExecutor implements SolverExecutor {

        @Override
        public SolverRun execute(KnapsackSolver solver, Instance instance, Budget budget, long seed) {
            return SolverRun.execute(solver, instance, budget, seed);
        }

        @Override
        public String description() {
            return "en la misma JVM, sin límite duro";
        }

        @Override
        public void close() {
        }
    }
}
