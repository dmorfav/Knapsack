package org.dmorfav.knapsack.core.generator;

import java.util.Objects;

/**
 * Parámetros de una instancia generada. La capacidad sigue la convención de Pisinger para la instancia h de una
 * serie de H: {@code C = floor(h · Σw / (H + 1))}.
 */
public record GeneratorSpec(InstanceClass type, int items, long range, int instanceNumber, int seriesSize, long seed) {

    public GeneratorSpec {
        Objects.requireNonNull(type, "type");
        if (items < 1) {
            throw new IllegalArgumentException("items debe ser >= 1, es " + items);
        }
        if (range < 10) {
            throw new IllegalArgumentException("range debe ser >= 10 para que R/10 sea al menos 1, es " + range);
        }
        if (seriesSize < 1 || instanceNumber < 1 || instanceNumber > seriesSize) {
            throw new IllegalArgumentException(
                    "se requiere 1 <= instanceNumber <= seriesSize, son " + instanceNumber + " y " + seriesSize);
        }
    }

    public String name() {
        return type.slug() + "-n" + items + "-r" + range + "-h" + instanceNumber + "of" + seriesSize + "-s" + seed;
    }
}
