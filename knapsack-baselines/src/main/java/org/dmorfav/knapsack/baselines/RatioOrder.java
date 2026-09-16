package org.dmorfav.knapsack.baselines;

import java.util.Comparator;
import java.util.stream.IntStream;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;

final class RatioOrder {

    private RatioOrder() {
    }

    /** Índices por ratio beneficio/peso decreciente; a igual ratio, por índice. Peso 0 cuenta como ratio infinito. */
    static int[] byDecreasingRatio(Instance instance) {
        Comparator<Integer> byRatio = (a, b) -> compareRatios(instance.item(b), instance.item(a));
        return IntStream.range(0, instance.size())
                .boxed()
                .sorted(byRatio.thenComparing(Comparator.naturalOrder()))
                .mapToInt(Integer::intValue)
                .toArray();
    }

    /** Compara x.profit/x.weight con y.profit/y.weight multiplicando en 128 bits, sin redondeos. */
    static int compareRatios(Item x, Item y) {
        long leftHigh = Math.multiplyHigh(x.profit(), y.weight());
        long rightHigh = Math.multiplyHigh(y.profit(), x.weight());
        if (leftHigh != rightHigh) {
            return Long.compare(leftHigh, rightHigh);
        }
        return Long.compareUnsigned(x.profit() * y.weight(), y.profit() * x.weight());
    }
}
