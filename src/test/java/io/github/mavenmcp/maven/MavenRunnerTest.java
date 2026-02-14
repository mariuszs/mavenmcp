package io.github.mavenmcp.maven;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MavenRunnerTest {

    private final MavenRunner runner = new MavenRunner();

    @Test
    void shouldExecuteMvnVersionSuccessfully() {
        // Use system mvn to run --version (works in any directory)
        Path mvn = MavenDetector.detect(Path.of("."));
        Path projectDir = Path.of(".").toAbsolutePath();

        MavenExecutionResult result = runner.execute("--version", List.of(), mvn, projectDir,
                MavenRunner.DEFAULT_TIMEOUT_MS);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.stdout()).contains("Apache Maven");
        assertThat(result.duration()).isGreaterThan(0);
        assertThat(result.timedOut()).isFalse();
    }

    @Test
    void shouldCaptureStdoutAndStderr() {
        Path mvn = MavenDetector.detect(Path.of("."));
        Path projectDir = Path.of(".").toAbsolutePath();

        MavenExecutionResult result = runner.execute("--version", List.of(), mvn, projectDir,
                MavenRunner.DEFAULT_TIMEOUT_MS);

        assertThat(result.stdout()).isNotNull();
        assertThat(result.stderr()).isNotNull();
        assertThat(result.timedOut()).isFalse();
    }

    @Test
    void shouldPassExtraArguments() {
        Path mvn = MavenDetector.detect(Path.of("."));
        Path projectDir = Path.of(".").toAbsolutePath();

        // -q (quiet) should reduce output
        MavenExecutionResult result = runner.execute("--version", List.of("-q"), mvn, projectDir,
                MavenRunner.DEFAULT_TIMEOUT_MS);

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.timedOut()).isFalse();
    }

    @Test
    void shouldThrowOnInvalidExecutable() {
        Path fakeExe = Path.of("/nonexistent/maven");
        Path projectDir = Path.of(".").toAbsolutePath();

        assertThatThrownBy(() -> runner.execute("compile", List.of(), fakeExe, projectDir,
                MavenRunner.DEFAULT_TIMEOUT_MS))
                .isInstanceOf(MavenExecutionException.class)
                .hasMessageContaining("Failed to start Maven process");
    }

    @Test
    void shouldReportNonZeroExitCode() {
        Path mvn = MavenDetector.detect(Path.of("."));
        Path projectDir = Path.of(".").toAbsolutePath();

        // Running an invalid goal should fail
        MavenExecutionResult result = runner.execute("nonexistent-goal-xyz", List.of(), mvn, projectDir,
                MavenRunner.DEFAULT_TIMEOUT_MS);

        assertThat(result.exitCode()).isNotEqualTo(0);
        assertThat(result.duration()).isGreaterThan(0);
        assertThat(result.timedOut()).isFalse();
    }

    @Test
    void shouldTimeoutAndKillProcess() {
        Path mvn = MavenDetector.detect(Path.of("."));
        Path projectDir = Path.of(".").toAbsolutePath();

        MavenExecutionResult result = runner.execute("--version", List.of(), mvn, projectDir, 1);

        assertThat(result.timedOut()).isTrue();
        assertThat(result.exitCode()).isEqualTo(-1);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.duration()).isGreaterThan(0);
    }
}
