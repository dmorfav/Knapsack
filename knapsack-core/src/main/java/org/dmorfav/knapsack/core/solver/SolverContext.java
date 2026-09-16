package org.dmorfav.knapsack.core.solver;

import java.time.Duration;
import java.util.random.RandomGenerator;

import org.dmorfav.knapsack.core.Evaluation;
import org.dmorfav.knapsack.core.Solution;

public interface SolverContext {

    /** Generador con semilla fija por ejecución: con la misma semilla, la misma secuencia. */
    RandomGenerator random();

    /** Cuenta como una evaluación; lanza {@link BudgetExhaustedException} si el presupuesto ya se agotó. */
    Evaluation evaluate(Solution solution);

    long evaluations();

    Duration elapsed();

    Budget budget();

    boolean budgetExhausted();
}
