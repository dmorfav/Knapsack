package org.dmorfav.knapsack.bench.experiment;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.dmorfav.knapsack.core.generator.GeneratorSpec;
import org.dmorfav.knapsack.core.generator.InstanceClass;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * Conjunto de instancias generadas: el producto cartesiano de clases, tamaños, posiciones en la serie (que fijan la
 * capacidad) y semillas. Los ficheros de la suite son siempre la salida del generador para esta definición.
 */
public record InstanceSuite(String name, String description, List<InstanceClass> classes, List<Integer> items,
                            long range, List<Integer> instanceNumbers, int seriesSize, List<Long> seeds) {

    private static final Pattern SLUG = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final YAMLMapper MAPPER = YAMLMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public InstanceSuite {
        Objects.requireNonNull(name, "name");
        if (!SLUG.matcher(name).matches()) {
            throw new IllegalArgumentException("name debe ser kebab-case en minúsculas, es '" + name + "'");
        }
        description = description == null ? "" : description;
        classes = requireNotEmpty(classes, "classes");
        items = requireNotEmpty(items, "items");
        instanceNumbers = requireNotEmpty(instanceNumbers, "instanceNumbers");
        seeds = requireNotEmpty(seeds, "seeds");
    }

    public static InstanceSuite read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return MAPPER.readValue(reader, SuiteFile.class).toSuite();
        } catch (JacksonException | IllegalArgumentException e) {
            throw new IOException("suite inválida en " + path + ": " + e.getMessage(), e);
        }
    }

    /** Una especificación por instancia, en orden de clase, tamaño, capacidad y semilla. */
    public List<GeneratorSpec> specs() {
        List<GeneratorSpec> specs = new ArrayList<>();
        for (InstanceClass type : classes) {
            for (int size : items) {
                for (int instanceNumber : instanceNumbers) {
                    for (long seed : seeds) {
                        specs.add(new GeneratorSpec(type, size, range, instanceNumber, seriesSize, seed));
                    }
                }
            }
        }
        return specs;
    }

    private static <T> List<T> requireNotEmpty(List<T> values, String field) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("'" + field + "' necesita al menos un valor");
        }
        return List.copyOf(values);
    }

    record SuiteFile(String name, String description, List<String> classes, List<Integer> items, Long range,
                     List<Integer> instanceNumbers, Integer series, List<Long> seeds) {

        InstanceSuite toSuite() {
            if (classes == null) {
                throw new IllegalArgumentException("falta 'classes'");
            }
            if (range == null || series == null) {
                throw new IllegalArgumentException("faltan 'range' o 'series'");
            }
            return new InstanceSuite(name, description, classes.stream().map(InstanceClass::fromSlug).toList(), items,
                    range, instanceNumbers, series, seeds);
        }
    }
}
