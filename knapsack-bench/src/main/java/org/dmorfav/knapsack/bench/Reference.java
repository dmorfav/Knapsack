package org.dmorfav.knapsack.bench;

import org.dmorfav.knapsack.baselines.UpperBounds;
import org.dmorfav.knapsack.core.Instance;

/** Valor contra el que se mide el GAP: el óptimo conocido o, si falta, la cota de Dantzig. */
public record Reference(long value, Kind kind) {

    public enum Kind {
        OPTIMUM, UPPER_BOUND
    }

    public static Reference of(Instance instance) {
        return instance.knownOptimum().isPresent()
                ? new Reference(instance.knownOptimum().getAsLong(), Kind.OPTIMUM)
                : new Reference(UpperBounds.dantzig(instance), Kind.UPPER_BOUND);
    }

    /** GAP en tanto por ciento: {@code 100 · (referencia − beneficio) / referencia}; 0 si la referencia es 0. */
    public double gapPercent(long profit) {
        return value == 0 ? 0 : 100.0 * (value - profit) / value;
    }
}
