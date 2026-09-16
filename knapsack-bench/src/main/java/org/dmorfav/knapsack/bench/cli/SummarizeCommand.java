package org.dmorfav.knapsack.bench.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import org.dmorfav.knapsack.bench.experiment.ResultsDirectory;
import org.dmorfav.knapsack.bench.experiment.RunRecord;
import org.dmorfav.knapsack.bench.stats.Comparison;
import org.dmorfav.knapsack.bench.stats.Summarizer;
import org.dmorfav.knapsack.bench.stats.SummaryOutput;
import org.dmorfav.knapsack.bench.stats.SummaryRow;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "summarize", mixinStandardHelpOptions = true,
        description = "Regenera summary.csv a partir del runs.jsonl de una carpeta de resultados y muestra la tabla y la comparación estadística.")
public final class SummarizeCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Parameters(paramLabel = "<resultados>", description = "Carpeta de resultados de un experimento.")
    Path directory;

    @Override
    public Integer call() throws IOException {
        PrintWriter out = spec.commandLine().getOut();
        ResultsDirectory results = new ResultsDirectory(directory);
        List<RunRecord> records = results.readRuns();
        List<SummaryRow> summary = Summarizer.summarize(records);
        SummaryOutput.writeCsv(summary, directory.resolve(ResultsDirectory.SUMMARY));
        SummaryOutput.printTable(summary, out);
        SummaryOutput.printComparison(Comparison.of(records), out);
        return 0;
    }
}
