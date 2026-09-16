package org.dmorfav.knapsack.core.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;

/**
 * Genera instancias con las distribuciones de Pisinger. Usa su propio generador aleatorio, así que reproduce las
 * distribuciones pero no los ficheros exactos publicados por Pisinger.
 */
public final class PisingerGenerator {

    private static final String RANDOM_ALGORITHM = "L64X128MixRandom";

    private PisingerGenerator() {
    }

    public static Instance generate(GeneratorSpec spec) {
        RandomGenerator random = RandomGeneratorFactory.of(RANDOM_ALGORITHM).create(spec.seed());
        long r = spec.range();
        List<Item> items = new ArrayList<>(spec.items());
        long totalWeight = 0;
        for (int i = 0; i < spec.items(); i++) {
            Item item = switch (spec.type()) {
                case UNCORRELATED -> {
                    long weight = uniform(random, 1, r);
                    yield new Item(uniform(random, 1, r), weight);
                }
                case WEAKLY_CORRELATED -> {
                    long weight = uniform(random, 1, r);
                    yield new Item(uniform(random, Math.max(1, weight - r / 10), weight + r / 10), weight);
                }
                case STRONGLY_CORRELATED -> {
                    long weight = uniform(random, 1, r);
                    yield new Item(weight + r / 10, weight);
                }
                case INVERSE_STRONGLY_CORRELATED -> {
                    long profit = uniform(random, 1, r);
                    yield new Item(profit, profit + r / 10);
                }
                case ALMOST_STRONGLY_CORRELATED -> {
                    long weight = uniform(random, 1, r);
                    yield new Item(uniform(random, weight + r / 10 - r / 500, weight + r / 10 + r / 500), weight);
                }
                case SUBSET_SUM -> {
                    long weight = uniform(random, 1, r);
                    yield new Item(weight, weight);
                }
                case UNCORRELATED_SIMILAR_WEIGHTS -> {
                    long weight = uniform(random, 100_000, 100_100);
                    yield new Item(uniform(random, 1, 1_000), weight);
                }
            };
            items.add(item);
            totalWeight += item.weight();
        }
        long capacity = Math.multiplyExact(spec.instanceNumber(), totalWeight) / (spec.seriesSize() + 1L);
        return new Instance(spec.name(), capacity, items);
    }

    private static long uniform(RandomGenerator random, long min, long max) {
        return random.nextLong(min, max + 1);
    }
}
