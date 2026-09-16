package org.dmorfav.knapsack.core.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalLong;

import org.dmorfav.knapsack.core.Instance;
import org.dmorfav.knapsack.core.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InstanceFilesTest {

    @TempDir
    Path tempDir;

    @Test
    void readsProfitAndWeightFromTheirOwnFields() throws Exception {
        Instance instance = InstanceFiles.read(resource("/instances/small-9.json"));

        assertThat(instance.name()).isEqualTo("small-9");
        assertThat(instance.capacity()).isEqualTo(100);
        assertThat(instance.size()).isEqualTo(9);
        assertThat(instance.item(0)).isEqualTo(new Item(11, 10));
        assertThat(instance.knownOptimum()).isEmpty();
    }

    @Test
    void writeThenReadRoundTrips() throws IOException {
        Instance original = new Instance("round-trip", 70,
                List.of(new Item(5, 3), new Item(8, 9)), OptionalLong.of(13));
        Path file = tempDir.resolve("round-trip.json");

        InstanceFiles.write(original, file);

        assertThat(InstanceFiles.read(file)).isEqualTo(original);
    }

    @Test
    void omitsUnknownOptimumWhenWriting() throws IOException {
        Path file = tempDir.resolve("no-optimum.json");

        InstanceFiles.write(new Instance("unsolved", 1, List.of(new Item(1, 1))), file);

        assertThat(Files.readString(file)).doesNotContain("\"optimum\"");
    }

    @Test
    void rejectsMissingCapacity() throws IOException {
        Path file = write("{\"name\": \"x\", \"items\": [{\"profit\": 1, \"weight\": 1}]}");

        assertThatThrownBy(() -> InstanceFiles.read(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void rejectsItemWithoutWeight() throws IOException {
        Path file = write("{\"name\": \"x\", \"capacity\": 1, \"items\": [{\"profit\": 1}]}");

        assertThatThrownBy(() -> InstanceFiles.read(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("weight");
    }

    @Test
    void rejectsUnknownFields() throws IOException {
        Path file = write("{\"name\": \"x\", \"capacity\": 1, \"capacidad\": 2, \"items\": [{\"profit\": 1, \"weight\": 1}]}");

        assertThatThrownBy(() -> InstanceFiles.read(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("capacidad");
    }

    @Test
    void rejectsTheLegacyArrayFormat() throws IOException {
        Path file = write("[{\"weight\": 1, \"profit\": 2}]");

        assertThatThrownBy(() -> InstanceFiles.read(file)).isInstanceOf(IOException.class);
    }

    private Path write(String json) throws IOException {
        Path file = tempDir.resolve("instance.json");
        Files.writeString(file, json);
        return file;
    }

    private static Path resource(String name) throws URISyntaxException {
        return Path.of(InstanceFilesTest.class.getResource(name).toURI());
    }
}
