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

    /**
     * Lo que produjo una llamada a {@code solve}, antes de validarlo. Es lo que viaja desde un proceso aislado:
     * {@code failure} solo recoge lo que ocurrió dentro del solver.
     */
    public record Attempt(Solution solution, long evaluations, Duration elapsed, List<TracePoint> trace, String failure) {

        public static Attempt failed(String failure, long evaluations, Duration elapsed) {
            return new Attempt(null, evaluations, elapsed, List.of(), failure);
        }
    }

    /** Ejecuta en este hilo. Un solver que no termina sin evaluar lo bloquea: para eso está el ejecutor aislado. */
    public static SolverRun execute(KnapsackSolver solver, Instance instance, Budget budget, long seed) {
        return of(solver.info(), seed, instance, attempt(solver, instance, budget, seed));
    }

    public static Attempt attempt(KnapsackSolver solver, Instance instance, Budget budget, long seed) {
        RunContext context = new RunContext(instance, budget, seed);
        Solution solution;
        try {
            solution = solver.solve(instance, context);
        } catch (BudgetExhaustedException e) {
            return failed(context, e.getMessage());
        } catch (RuntimeException | StackOverflowError | OutOfMemoryError e) {
            return failed(context, "excepción " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return new Attempt(solution, context.evaluations(), context.elapsed(), context.trace(), null);
    }

    /** Valida un intento contra la instancia: tamaño, factibilidad y óptimo conocido. */
    public static SolverRun of(SolverInfo info, long seed, Instance instance, Attempt attempt) {
        if (attempt.failure() != null) {
            return failed(info, seed, attempt, attempt.failure());
        }
        Solution solution = attempt.solution();
        if (solution == null) {
            return failed(info, seed, attempt, "devolvió null");
        }
        if (solution.size() != instance.size()) {
            return failed(info, seed, attempt,
                    "devolvió " + solution.size() + " posiciones y la instancia tiene " + instance.size());
        }
        Evaluation evaluation = instance.evaluate(solution);
        return new SolverRun(info, seed, solution, evaluation, attempt.evaluations(), attempt.elapsed(),
                attempt.trace(), validate(evaluation, instance));
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

    private static Attempt failed(RunContext context, String failure) {
        return new Attempt(null, context.evaluations(), context.elapsed(), context.trace(), failure);
    }

    private static SolverRun failed(SolverInfo info, long seed, Attempt attempt, String failure) {
        return new SolverRun(info, seed, null, null, attempt.evaluations(), attempt.elapsed(), attempt.trace(), failure);
    }
}
