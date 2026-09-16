package org.dmorfav.knapsack.baselines;

import java.math.BigInteger;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;

public final class UpperBounds {

    private UpperBounds() {
    }

    /**
     * Cota de Dantzig: óptimo de la relajación lineal, redondeado hacia abajo. Ninguna solución 0/1 la supera,
     * así que sirve para acotar el GAP cuando el óptimo exacto no es calculable.
     */
    public static long dantzig(Instance instance) {
        long remaining = instance.capacity();
        long profit = 0;
        for (int index : RatioOrder.byDecreasingRatio(instance)) {
            Item item = instance.item(index);
            if (item.weight() <= remaining) {
                profit += item.profit();
                remaining -= item.weight();
            } else {
                profit += BigInteger.valueOf(item.profit())
                        .multiply(BigInteger.valueOf(remaining))
                        .divide(BigInteger.valueOf(item.weight()))
                        .longValueExact();
                break;
            }
        }
        return profit;
    }
}
