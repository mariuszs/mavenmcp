package io.github.mavenmcp.maven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes Maven commands as child processes and captures their output.
 * <p>
 * Stdout and stderr are consumed on separate threads via {@link CompletableFuture}
 * to prevent deadlock when Maven produces large output on both streams.
 */
public class MavenRunner {

    private static final Logger log = LoggerFactory.getLogger(MavenRunner.class);

    /** Default timeout for Maven executions in milliseconds (2 minutes). */
    public static final int DEFAULT_TIMEOUT_MS = 120_000;

    /**
     * Execute a Maven goal as a child process.
     *
     * @param goal            the Maven goal to execute (e.g., "compile", "test")
     * @param extraArgs       additional Maven CLI arguments (e.g., ["-DskipTests"])
     * @param mavenExecutable path to the Maven executable (mvnw or mvn)
     * @param projectDir      the project working directory
     * @param timeoutMs       maximum time to wait for the process in milliseconds
     * @return the execution result with exit code, captured output, and duration
     * @throws MavenExecutionException if the process cannot be started
     */
    public MavenExecutionResult execute(String goal, List<String> extraArgs,
                                        Path mavenExecutable, Path projectDir, int timeoutMs) {
        List<String> command = buildCommand(mavenExecutable, goal, extraArgs);
        log.info("Executing: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(projectDir.toFile());
        // Do NOT redirect streams — we capture them separately

        long startTime = System.currentTimeMillis();
        try {
            Process process = pb.start();

            // Consume stdout and stderr concurrently to prevent deadlock
            CompletableFuture<String> stdoutFuture = readStreamAsync(process.getInputStream());
            CompletableFuture<String> stderrFuture = readStreamAsync(process.getErrorStream());

            boolean finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            long duration = System.currentTimeMillis() - startTime;

            if (!finished) {
                log.warn("Maven process timed out after {}ms, killing", timeoutMs);
                process.destroyForcibly();
                String stdout = stdoutFuture.getNow("");
                String stderr = stderrFuture.getNow("");
                return new MavenExecutionResult(-1, stdout, stderr, duration, true);
            }

            int exitCode = process.exitValue();
            String stdout = stdoutFuture.join();
            String stderr = stderrFuture.join();

            log.info("Maven exited with code {} in {}ms", exitCode, duration);
            return new MavenExecutionResult(exitCode, stdout, stderr, duration, false);

        } catch (IOException e) {
            long duration = System.currentTimeMillis() - startTime;
            throw new MavenExecutionException(
                    "Failed to start Maven process: " + e.getMessage(), e, duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            long duration = System.currentTimeMillis() - startTime;
            throw new MavenExecutionException(
                    "Maven process interrupted", e, duration);
        }
    }

    private List<String> buildCommand(Path mavenExecutable, String goal, List<String> extraArgs) {
        List<String> command = new ArrayList<>();
        command.add(mavenExecutable.toString());
        command.add(goal);
        command.add("-B"); // batch mode — always
        if (extraArgs != null) {
            command.addAll(extraArgs);
        }
        return command;
    }

    private CompletableFuture<String> readStreamAsync(InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                return reader.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                log.warn("Error reading process stream: {}", e.getMessage());
                return "";
            }
        });
    }
}
