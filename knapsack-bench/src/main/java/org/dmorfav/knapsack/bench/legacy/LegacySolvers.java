package org.dmorfav.knapsack.bench.legacy;

import java.util.function.Function;

import algorithms.DiscreteBinaryHarmonySearch;
import algorithms.HarmonySearch;
import algorithms.HillClimbingBestAscent;
import algorithms.HillClimbingFirstAscent;
import algorithms.ImprovedHarmonySearch;
import model.Knapsack;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.KnapsackSolver;
import org.dmorfav.knapsack.core.solver.SolverContext;
import org.dmorfav.knapsack.core.solver.SolverInfo;

/**
 * Adaptadores de los algoritmos anteriores al contrato {@link KnapsackSolver}, sin modificarlos.
 * Usan Math.random, así que ignoran la semilla: sus resultados no son reproducibles.
 */
public final class LegacySolvers {

    static final String VARIANT = "legacy";
    static final String AUTHOR = "Daniel Morfa Vega";

    private LegacySolvers() {
    }

    abstract static class LegacySolver implements KnapsackSolver {

        private final SolverInfo info;
        private final Function<Knapsack, int[]> algorithm;

        LegacySolver(String family, Function<Knapsack, int[]> algorithm) {
            this.info = new SolverInfo(family, VARIANT, AUTHOR);
            this.algorithm = algorithm;
        }

        @Override
        public final SolverInfo info() {
            return info;
        }

        @Override
        public final Solution solve(Instance instance, SolverContext context) {
            int[] assignment = algorithm.apply(new CountingKnapsack(instance, context));
            return CountingKnapsack.toSolution(assignment, instance.size());
        }
    }

    public static final class LegacyHarmonySearch extends LegacySolver {
        public LegacyHarmonySearch() {
            super("harmony-search", knapsack -> new HarmonySearch(knapsack).HarmonySearchAlgorithm());
        }
    }

    public static final class LegacyImprovedHarmonySearch extends LegacySolver {
        public LegacyImprovedHarmonySearch() {
            super("improved-harmony-search", knapsack -> new ImprovedHarmonySearch(knapsack).HarmonySearchAlgorithm());
        }
    }

    public static final class LegacyDiscreteBinaryHarmonySearch extends LegacySolver {
        public LegacyDiscreteBinaryHarmonySearch() {
            super("discrete-binary-harmony-search",
                    knapsack -> new DiscreteBinaryHarmonySearch(knapsack).HarmonySearchAlgorithm());
        }
    }

    public static final class LegacyHillClimbingFirstAscent extends LegacySolver {
        public LegacyHillClimbingFirstAscent() {
            super("hill-climbing-first-ascent", knapsack -> new HillClimbingFirstAscent(knapsack).climbTheHill());
        }
    }

    public static final class LegacyHillClimbingBestAscent extends LegacySolver {
        public LegacyHillClimbingBestAscent() {
            super("hill-climbing-best-ascent", knapsack -> new HillClimbingBestAscent(knapsack).climbTheHill());
        }
    }
}
