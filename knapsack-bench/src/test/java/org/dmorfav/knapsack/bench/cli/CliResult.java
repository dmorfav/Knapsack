package org.dmorfav.knapsack.bench.cli;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.dmorfav.knapsack.bench.Main;
import picocli.CommandLine;

record CliResult(int exitCode, String out, String err) {

    static CliResult execute(String... args) {
        StringWriter out = new StringWriter();
        StringWriter err = new StringWriter();
        CommandLine commandLine = Main.commandLine();
        commandLine.setOut(new PrintWriter(out));
        commandLine.setErr(new PrintWriter(err));
        int exitCode = commandLine.execute(args);
        return new CliResult(exitCode, out.toString(), err.toString());
    }
}
