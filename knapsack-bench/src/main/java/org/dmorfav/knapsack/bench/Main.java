package org.dmorfav.knapsack.bench;

import org.dmorfav.knapsack.bench.cli.ExperimentCommand;
import org.dmorfav.knapsack.bench.cli.GenerateCommand;
import org.dmorfav.knapsack.bench.cli.OptimumCommand;
import org.dmorfav.knapsack.bench.cli.RunCommand;
import org.dmorfav.knapsack.bench.cli.SummarizeCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
        name = "knapsack",
        description = "Banco de pruebas de metaheurísticas para la mochila 0/1.",
        mixinStandardHelpOptions = true,
        subcommands = {RunCommand.class, ExperimentCommand.class, SummarizeCommand.class, GenerateCommand.class,
                OptimumCommand.class, CommandLine.HelpCommand.class})
public final class Main implements Runnable {

    @Spec
    CommandSpec spec;

    public static void main(String[] args) {
        System.exit(commandLine().execute(args));
    }

    public static CommandLine commandLine() {
        return new CommandLine(new Main()).setCaseInsensitiveEnumValuesAllowed(true);
    }

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}
