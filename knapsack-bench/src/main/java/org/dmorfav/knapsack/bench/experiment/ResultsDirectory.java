package org.dmorfav.knapsack.bench.experiment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Carpeta de resultados de un experimento. runs.jsonl es la fuente de verdad; el resumen se deriva de ella y se
 * puede regenerar en cualquier momento.
 */
public final class ResultsDirectory {

    public static final String MANIFEST = "manifest.json";
    public static final String RUNS = "runs.jsonl";
    public static final String TRACES = "traces.jsonl";
    public static final String SUMMARY = "summary.csv";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final JsonMapper PRETTY = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private final Path path;

    public ResultsDirectory(Path path) {
        this.path = path;
    }

    public Path path() {
        return path;
    }

    public void writeManifest(Manifest manifest) throws IOException {
        Files.writeString(path.resolve(MANIFEST), PRETTY.writeValueAsString(manifest) + System.lineSeparator());
    }

    public List<RunRecord> readRuns() throws IOException {
        Path runs = path.resolve(RUNS);
        if (!Files.isRegularFile(runs)) {
            throw new IOException("no existe " + runs);
        }
        try (Stream<String> lines = Files.lines(runs)) {
            return lines.filter(line -> !line.isBlank()).map(line -> MAPPER.readValue(line, RunRecord.class)).toList();
        }
    }

    JsonLinesWriter openJsonLines(String fileName) throws IOException {
        return new JsonLinesWriter(Files.newBufferedWriter(path.resolve(fileName)));
    }

    /** Escribe una línea por registro y vacía el búfer: si el proceso muere, lo ya ejecutado sigue en disco. */
    static final class JsonLinesWriter implements AutoCloseable {

        private final BufferedWriter writer;

        private JsonLinesWriter(BufferedWriter writer) {
            this.writer = writer;
        }

        void append(Object record) throws IOException {
            writer.write(MAPPER.writeValueAsString(record));
            writer.newLine();
            writer.flush();
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }
    }
}
