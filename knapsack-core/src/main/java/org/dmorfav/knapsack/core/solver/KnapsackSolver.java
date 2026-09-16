package org.dmorfav.knapsack.core.solver;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;

/**
 * Contrato de un algoritmo. Una implementación devuelve una solución de {@code instance.size()} posiciones,
 * toma toda la aleatoriedad de {@link SolverContext#random()}, evalúa con {@link SolverContext#evaluate}
 * y termina en cuanto {@link SolverContext#budgetExhausted()} es cierto. Se registra en
 * {@code META-INF/services/org.dmorfav.knapsack.core.solver.KnapsackSolver} con un constructor público sin argumentos.
 */
public interface KnapsackSolver {

    SolverInfo info();

    Solution solve(Instance instance, SolverContext context);
}
