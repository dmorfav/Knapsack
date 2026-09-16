package org.dmorfav.knapsack.bench.legacy;

import java.util.ArrayList;

import model.Item;
import model.Knapsack;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Solution;
import org.dmorfav.knapsack.core.solver.SolverContext;

/**
 * Modelo antiguo cuyas consultas pasan por {@link SolverContext#evaluate}: cada llamada a evaluateOF,
 * evaluateRestriction o totalWeight cuenta como una evaluación y respeta el presupuesto.
 */
final class CountingKnapsack extends Knapsack {

    private final SolverContext context;
    private final int size;

    CountingKnapsack(Instance instance, SolverContext context) {
        super(instance.capacity(), legacyItems(instance));
        this.context = context;
        this.size = instance.size();
    }

    @Override
    public float evaluateOF(int[] assignment) {
        return context.evaluate(toSolution(assignment, size)).profit();
    }

    @Override
    public boolean evaluateRestriction(int[] assignment) {
        return context.evaluate(toSolution(assignment, size)).feasible();
    }

    @Override
    public float totalWeight(int[] assignment) {
        return context.evaluate(toSolution(assignment, size)).weight();
    }

    /** Misma semántica que el modelo antiguo: solo el valor 1 selecciona, y se ignoran posiciones extra. */
    static Solution toSolution(int[] assignment, int size) {
        boolean[] selected = new boolean[size];
        for (int i = 0; i < size; i++) {
            selected[i] = assignment[i] == 1;
        }
        return Solution.of(selected);
    }

    private static ArrayList<Item> legacyItems(Instance instance) {
        ArrayList<Item> items = new ArrayList<>(instance.size());
        instance.items().forEach(item -> items.add(new Item(item.profit(), item.weight())));
        return items;
    }
}
