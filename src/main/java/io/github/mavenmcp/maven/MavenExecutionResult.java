package io.github.mavenmcp.maven;

/**
 * Raw result of a Maven process execution.
 *
 * @param exitCode process exit code (0 = success)
 * @param stdout   complete captured standard output
 * @param stderr   complete captured standard error
 * @param duration wall-clock execution time in milliseconds
 * @param timedOut true if the process was killed due to timeout
 */
public record MavenExecutionResult(int exitCode, String stdout, String stderr, long duration, boolean timedOut) {

    /**
     * @return true if Maven exited successfully (exit code 0 and not timed out)
     */
    public boolean isSuccess() {
        return exitCode == 0 && !timedOut;
    }
}
