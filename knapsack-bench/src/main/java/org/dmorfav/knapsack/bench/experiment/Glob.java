package org.dmorfav.knapsack.bench.experiment;

import java.util.regex.Pattern;

/** Patrones con {@code *}, {@code **} y {@code ?}, iguales en cualquier sistema operativo (siempre con '/'). */
final class Glob {

    private Glob() {
    }

    static boolean hasWildcards(String pattern) {
        return pattern.contains("*") || pattern.contains("?");
    }

    /** Para rutas: {@code *} y {@code ?} no cruzan '/', {@code **} sí. */
    static Pattern forPaths(String pattern) {
        return compile(pattern, "[^/]*", "[^/]");
    }

    /** Para ids de solver ({@code familia/variante}): {@code *} cruza '/', así que "*" selecciona todo. */
    static Pattern forIds(String pattern) {
        return compile(pattern, ".*", ".");
    }

    private static Pattern compile(String pattern, String star, String question) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (pattern.startsWith("**/", i)) {
                regex.append("(?:.*/)?");
                i += 2;
            } else if (pattern.startsWith("**", i)) {
                regex.append(".*");
                i++;
            } else if (c == '*') {
                regex.append(star);
            } else if (c == '?') {
                regex.append(question);
            } else {
                regex.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(regex.toString());
    }
}
