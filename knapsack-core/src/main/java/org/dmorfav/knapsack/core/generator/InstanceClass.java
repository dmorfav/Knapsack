package org.dmorfav.knapsack.core.generator;

import java.util.Arrays;
import java.util.stream.Collectors;

/** Clases de instancias de Pisinger (2005, "Where are the hard knapsack problems?"), con rango de coeficientes R. */
public enum InstanceClass {

    UNCORRELATED("uncorrelated"),
    WEAKLY_CORRELATED("weakly-correlated"),
    STRONGLY_CORRELATED("strongly-correlated"),
    INVERSE_STRONGLY_CORRELATED("inverse-strongly-correlated"),
    ALMOST_STRONGLY_CORRELATED("almost-strongly-correlated"),
    SUBSET_SUM("subset-sum"),
    /** Ignora R: pesos en [100000, 100100] y beneficios en [1, 1000]. */
    UNCORRELATED_SIMILAR_WEIGHTS("uncorrelated-similar-weights");

    private final String slug;

    InstanceClass(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public static InstanceClass fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(type -> type.slug.equals(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("clase de instancia desconocida '" + slug
                        + "'; opciones: " + Arrays.stream(values()).map(InstanceClass::slug).collect(Collectors.joining(", "))));
    }
}
