package org.dmorfav.knapsack.bench.experiment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Dónde y con qué se ejecutó un experimento. Los campos que no se puedan averiguar quedan en null. */
public record EnvironmentInfo(String javaVersion, String javaVendor, String javaVm, String os, String arch,
                              int availableProcessors, long maxMemoryBytes, String cpuModel, String gitCommit,
                              Boolean gitDirty) {

    static EnvironmentInfo capture(Path root) {
        Runtime runtime = Runtime.getRuntime();
        String githubSha = System.getenv("GITHUB_SHA");
        return new EnvironmentInfo(
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version"),
                System.getProperty("os.name") + " " + System.getProperty("os.version"),
                System.getProperty("os.arch"),
                runtime.availableProcessors(),
                runtime.maxMemory(),
                detectCpuModel(),
                githubSha != null ? githubSha : command(root, "git", "rev-parse", "HEAD"),
                detectGitDirty(root));
    }

    private static String detectCpuModel() {
        Path cpuinfo = Path.of("/proc/cpuinfo");
        if (Files.isReadable(cpuinfo)) {
            try {
                return Files.readAllLines(cpuinfo).stream()
                        .filter(line -> line.startsWith("model name"))
                        .map(line -> line.substring(line.indexOf(':') + 1).trim())
                        .findFirst()
                        .orElse(null);
            } catch (IOException e) {
                return null;
            }
        }
        return command(Path.of("."), "sysctl", "-n", "machdep.cpu.brand_string");
    }

    private static Boolean detectGitDirty(Path root) {
        String status = command(root, "git", "status", "--porcelain");
        return status == null ? null : !status.isEmpty();
    }

    private static String command(Path directory, String... command) {
        try {
            Process process = new ProcessBuilder(List.of(command))
                    .directory(directory.toFile())
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return process.exitValue() == 0 ? output : null;
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
