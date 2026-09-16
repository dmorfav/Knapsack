package org.dmorfav.knapsack.bench;

import java.time.Duration;

import org.dmorfav.knapsack.core.Evaluation;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.BudgetExhaustedException;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.RunContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;

/** Resultado de ejecutar un solver una vez. {@code failure} es null solo si devolvió una solución válida. */
public record SolverRun(SolverInfo info, Solution solution, Evaluation evaluation, long evaluations, Duration elapsed,
                        String failure) {

    public static SolverRun execute(KnapsackSolver solver, Instance instance, Budget budget, long seed) {
        RunContext context = new RunContext(instance, budget, seed);
        Solution solution;
        try {
            solution = solver.solve(instance, context);
        } catch (BudgetExhaustedException e) {
            return failed(solver, context, e.getMessage());
        } catch (RuntimeException e) {
            return failed(solver, context, "excepción " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        if (solution == null) {
            return failed(solver, context, "devolvió null");
        }
        if (solution.size() != instance.size()) {
            return failed(solver, context,
                    "devolvió " + solution.size() + " posiciones y la instancia tiene " + instance.size());
        }
        Evaluation evaluation = instance.evaluate(solution);
        return new SolverRun(solver.info(), solution, evaluation, context.evaluations(), context.elapsed(),
                validate(evaluation, instance));
    }

    public boolean succeeded() {
        return failure == null;
    }

    private static String validate(Evaluation evaluation, Instance instance) {
        if (!evaluation.feasible()) {
            return "solución no factible: peso " + evaluation.weight() + " > capacidad " + instance.capacity();
        }
        if (instance.knownOptimum().isPresent() && evaluation.profit() > instance.knownOptimum().getAsLong()) {
            return "beneficio " + evaluation.profit() + " supera el óptimo conocido " + instance.knownOptimum().getAsLong()
                    + ": la instancia o el solver es incorrecto";
        }
        return null;
    }

    private static SolverRun failed(KnapsackSolver solver, RunContext context, String failure) {
        return new SolverRun(solver.info(), null, null, context.evaluations(), context.elapsed(), failure);
    }
}
