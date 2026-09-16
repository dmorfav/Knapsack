package org.dmorfav.knapsack.bench;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dmorfav.knapsack.bench.execution.IsolatedExecutor;
import org.dmorfav.knapsack.baselines.DynamicProgrammingSolver;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.dmorfav.knapsack.core.generator.GeneratorSpec;
import org.dmorfav.knapsack.core.generator.InstanceClass;
import org.dmorfav.knapsack.core.generator.PisingerGenerator;
import org.dmorfav.knapsack.core.solver.Budget;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Contrato que cumple todo solver registrado, ejecutado como en un experimento: cada solver en su propio proceso,
 * así que uno que no termina falla su test en lugar de colgar el build. Los algoritmos originales
 * ({@code bench.legacy}) ignoran la semilla y el presupuesto, y solo se les exige terminar con soluciones válidas.
 */
class SolverContractTest {

    private static final String BASELINE_AUTHOR = "baseline";
    private static final Budget GENEROUS = new Budget(20_000, Duration.ofSeconds(10));

    private static IsolatedExecutor executor;

    @BeforeAll
    static void startExecutor() {
        executor = IsolatedExecutor.forCurrentJvm(Duration.ofSeconds(5));
    }

    @AfterAll
    static void stopExecutor() {
        executor.close();
    }

    static Stream<Named<KnapsackSolver>> solvers() {
        return SolverCatalog.discover().stream().map(solver -> Named.of(solver.info().id(), solver));
    }

    @Test
    void solverIdsAreUnique() {
        List<String> ids = SolverCatalog.discover().stream().map(solver -> solver.info().id()).toList();

        assertThat(ids).doesNotHaveDuplicates();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("solvers")
    void onlyBaselinesUseTheBaselineAuthor(KnapsackSolver solver) {
        boolean isBaseline = solver.getClass().getPackageName().startsWith("org.dmorfav.knapsack.baselines");

        assertThat(solver.info().author().equals(BASELINE_AUTHOR))
                .as("el autor '%s' está reservado a knapsack-baselines", BASELINE_AUTHOR)
                .isEqualTo(isBaseline);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("solvers")
    void returnsValidSolutionsOnEdgeAndGeneratedInstances(KnapsackSolver solver) {
        List<String> failures = new ArrayList<>();
        for (Instance instance : contractInstances()) {
            SolverRun run = executor.execute(solver, instance, GENEROUS, 1);
            if (!run.succeeded()) {
                failures.add(instance.name() + ": " + run.failure());
            }
        }

        assertThat(failures).as("ejecuciones fallidas de %s", solver.info().id()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("solvers")
    void sameSeedGivesTheSameRun(KnapsackSolver solver) {
        assumeFalse(isOriginal(solver), "los algoritmos originales usan Math.random");
        // Solo presupuesto de evaluaciones: uno de tiempo haría depender el resultado de la máquina
        Budget budget = Budget.ofEvaluations(5_000).withMaxTime(Duration.ofSeconds(10));
        Instance instance = generated(InstanceClass.UNCORRELATED, 60, 3);

        SolverRun first = executor.execute(solver, instance, budget, 42);
        SolverRun second = executor.execute(solver, instance, budget, 42);

        assertThat(first.succeeded()).as(first.failure()).isTrue();
        assertThat(second.solution()).isEqualTo(first.solution());
        assertThat(second.evaluations()).isEqualTo(first.evaluations());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("solvers")
    void stopsWithinTheEvaluationBudget(KnapsackSolver solver) {
        assumeFalse(isOriginal(solver), "los algoritmos originales solo paran cuando una evaluación lanza la excepción");
        Instance instance = generated(InstanceClass.STRONGLY_CORRELATED, 200, 4);

        SolverRun run = executor.execute(solver, instance, Budget.ofEvaluations(50).withMaxTime(Duration.ofSeconds(10)), 1);

        assertThat(run.succeeded()).as(run.failure()).isTrue();
        assertThat(run.evaluations()).isLessThanOrEqualTo(50);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("solvers")
    void stopsWithinTheTimeBudget(KnapsackSolver solver) {
        assumeFalse(isOriginal(solver), "los algoritmos originales solo paran cuando una evaluación lanza la excepción");
        Instance instance = generated(InstanceClass.WEAKLY_CORRELATED, 200, 5);

        SolverRun run = executor.execute(solver, instance, Budget.ofTime(Duration.ofMillis(200)), 1);

        assertThat(run.succeeded()).as(run.failure()).isTrue();
        assertThat(run.elapsed()).isLessThan(Duration.ofSeconds(2));
    }

    private static boolean isOriginal(KnapsackSolver solver) {
        return solver.getClass().getPackageName().equals("org.dmorfav.knapsack.bench.legacy");
    }

    /** Casos límite con óptimo evidente e instancias de Pisinger pequeñas con su óptimo exacto. */
    private static List<Instance> contractInstances() {
        List<Instance> instances = new ArrayList<>(List.of(
                new Instance("un-objeto-cabe", 10, List.of(new Item(5, 4))).withKnownOptimum(5),
                new Instance("un-objeto-no-cabe", 3, List.of(new Item(5, 4))).withKnownOptimum(0),
                new Instance("todos-caben", 100, List.of(new Item(3, 2), new Item(4, 3), new Item(5, 4)))
                        .withKnownOptimum(12),
                new Instance("ninguno-cabe", 1, List.of(new Item(3, 2), new Item(4, 3), new Item(5, 4)))
                        .withKnownOptimum(0)));
        int seed = 1;
        for (InstanceClass type : List.of(InstanceClass.UNCORRELATED, InstanceClass.STRONGLY_CORRELATED,
                InstanceClass.SUBSET_SUM)) {
            instances.add(generated(type, 40, seed++));
        }
        return instances;
    }

    private static Instance generated(InstanceClass type, int items, long seed) {
        Instance instance = PisingerGenerator.generate(new GeneratorSpec(type, items, 100, 1, 2, seed));
        return instance.withKnownOptimum(instance.evaluate(DynamicProgrammingSolver.optimum(instance)).profit());
    }
}
