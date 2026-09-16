package org.dmorfav.knapsack.core.solver;

import java.util.Objects;
import java.util.regex.Pattern;

/** Identidad de una implementación: {@code family} agrupa variantes del mismo algoritmo, p. ej. harmony-search/improved. */
public record SolverInfo(String family, String variant, String author) {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");

    public SolverInfo {
        requireSlug(family, "family");
        requireSlug(variant, "variant");
        Objects.requireNonNull(author, "author");
        if (author.isBlank()) {
            throw new IllegalArgumentException("author no puede estar vacío");
        }
    }

    public String id() {
        return family + "/" + variant;
    }

    private static void requireSlug(String value, String field) {
        Objects.requireNonNull(value, field);
        if (!SLUG.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    field + " debe ser kebab-case en minúsculas (p. ej. harmony-search), es '" + value + "'");
        }
    }
}
