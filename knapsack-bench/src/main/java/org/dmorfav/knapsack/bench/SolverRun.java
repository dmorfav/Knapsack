package org.dmorfav.knapsack.bench;

import java.time.Duration;
import java.util.List;

import org.dmorfav.knapsack.core.Evaluation;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.BudgetExhaustedException;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.RunContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;
import org.dmorfav.knapsack.core.solver.TracePoint;

/** Resultado de ejecutar un solver una vez. {@code failure} es null solo si devolvió una solución válida. */
public record SolverRun(SolverInfo info, long seed, Solution solution, Evaluation evaluation, long evaluations,
                        Duration elapsed, List<TracePoint> trace, String failure) {

    public static SolverRun execute(KnapsackSolver solver, Instance instance, Budget budget, long seed) {
        RunContext context = new RunContext(instance, budget, seed);
        Solution solution;
        try {
            solution = solver.solve(instance, context);
        } catch (BudgetExhaustedException e) {
            return failed(solver, seed, context, e.getMessage());
        } catch (RuntimeException e) {
            return failed(solver, seed, context, "excepción " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        Duration elapsed = context.elapsed();
        if (solution == null) {
            return failed(solver, seed, context, "devolvió null");
        }
        if (solution.size() != instance.size()) {
            return failed(solver, seed, context,
                    "devolvió " + solution.size() + " posiciones y la instancia tiene " + instance.size());
        }
        Evaluation evaluation = instance.evaluate(solution);
        return new SolverRun(solver.info(), seed, solution, evaluation, context.evaluations(), elapsed,
                context.trace(), validate(evaluation, instance));
    }

    public boolean succeeded() {
        return failure == null;
    }

    /** Primer punto de la traza que alcanzó el beneficio devuelto, o null si el solver no lo evaluó. */
    public TracePoint firstReachOfFinalProfit() {
        if (evaluation == null) {
            return null;
        }
        return trace.stream().filter(point -> point.profit() >= evaluation.profit()).findFirst().orElse(null);
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

    private static SolverRun failed(KnapsackSolver solver, long seed, RunContext context, String failure) {
        return new SolverRun(solver.info(), seed, null, null, context.evaluations(), context.elapsed(), context.trace(),
                failure);
    }
}
