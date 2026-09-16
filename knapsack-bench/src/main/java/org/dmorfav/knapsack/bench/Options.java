package org.dmorfav.knapsack.bench;

import java.nio.file.Path;
import java.time.Duration;

import org.dmorfav.knapsack.core.solver.Budget;

record Options(Path instance, long seed, Budget budget) {

    static final String USAGE = """
            Uso: ./mvnw compile exec:java -Dexec.args="[opciones]"
              --instance=<ruta>          instancia JSON (por defecto instances/legacy-20.json)
              --seed=<n>                 semilla de la ejecución (por defecto 1)
              --max-evaluations=<n>      evaluaciones máximas por algoritmo (por defecto sin límite)
              --max-seconds=<n>          segundos máximos por algoritmo (por defecto 60)""";

    static final Path DEFAULT_INSTANCE = Path.of("instances", "legacy-20.json");
    static final long DEFAULT_SEED = 1;
    static final Duration DEFAULT_MAX_TIME = Duration.ofSeconds(60);

    static Options parse(String... args) {
        Path instance = DEFAULT_INSTANCE;
        long seed = DEFAULT_SEED;
        Budget budget = Budget.ofTime(DEFAULT_MAX_TIME);
        for (String arg : args) {
            int equals = arg.indexOf('=');
            if (!arg.startsWith("--") || equals < 0) {
                throw new IllegalArgumentException("argumento no reconocido: " + arg);
            }
            String name = arg.substring(2, equals);
            String value = arg.substring(equals + 1);
            switch (name) {
                case "instance" -> instance = Path.of(value);
                case "seed" -> seed = parseLong(name, value);
                case "max-evaluations" -> budget = budget.withMaxEvaluations(parseLong(name, value));
                case "max-seconds" -> budget = budget.withMaxTime(Duration.ofSeconds(parseLong(name, value)));
                default -> throw new IllegalArgumentException("opción desconocida: --" + name);
            }
        }
        return new Options(instance, seed, budget);
    }

    private static long parseLong(String name, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--" + name + " espera un número entero, recibió '" + value + "'");
        }
    }
}
