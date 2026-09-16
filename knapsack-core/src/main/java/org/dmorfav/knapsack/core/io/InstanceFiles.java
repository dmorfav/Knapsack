package org.dmorfav.knapsack.core.io;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/** Formato: {@code {"name", "capacity", "optimum"?, "items": [{"profit", "weight"}]}}. */
public final class InstanceFiles {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private InstanceFiles() {
    }

    public static Instance read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return MAPPER.readValue(reader, InstanceJson.class).toInstance();
        } catch (JacksonException | IllegalArgumentException e) {
            throw new IOException("instancia inválida en " + path + ": " + e.getMessage(), e);
        }
    }

    public static void write(Instance instance, Path path) throws IOException {
        try {
            Files.writeString(path, MAPPER.writeValueAsString(InstanceJson.from(instance)) + System.lineSeparator());
        } catch (JacksonException e) {
            throw new IOException("no se pudo escribir la instancia en " + path + ": " + e.getMessage(), e);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record InstanceJson(String name, Long capacity, Long optimum, List<ItemJson> items) {

        static InstanceJson from(Instance instance) {
            List<ItemJson> items = instance.items().stream()
                    .map(item -> new ItemJson(item.profit(), item.weight()))
                    .toList();
            Long optimum = instance.knownOptimum().isPresent() ? instance.knownOptimum().getAsLong() : null;
            return new InstanceJson(instance.name(), instance.capacity(), optimum, items);
        }

        Instance toInstance() {
            require(name, "name");
            require(capacity, "capacity");
            require(items, "items");
            List<Item> parsed = items.stream().map(ItemJson::toItem).toList();
            OptionalLong knownOptimum = optimum == null ? OptionalLong.empty() : OptionalLong.of(optimum);
            return new Instance(name, capacity, parsed, knownOptimum);
        }
    }

    record ItemJson(Long profit, Long weight) {

        Item toItem() {
            require(profit, "profit");
            require(weight, "weight");
            return new Item(profit, weight);
        }
    }

    private static void require(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("falta el campo '" + field + "'");
        }
    }
}
