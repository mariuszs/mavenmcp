package io.github.mavenmcp;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapTest {

    @Test
    void bootstrapClassHasJava11BytecodeVersion() throws IOException {
        // Java 11 = major version 55
        int expectedMajorVersion = 55;

        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("io/github/mavenmcp/Bootstrap.class")) {
            assertThat(is).as("Bootstrap.class should be on classpath").isNotNull();

            DataInputStream dis = new DataInputStream(is);
            int magic = dis.readInt();
            assertThat(magic).as("Java class magic number").isEqualTo(0xCAFEBABE);

            dis.readUnsignedShort(); // minor version
            int majorVersion = dis.readUnsignedShort();

            assertThat(majorVersion)
                    .as("Bootstrap.class major version (Java 11 = 55)")
                    .isEqualTo(expectedMajorVersion);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {8, 11, 17, 20})
    void checkVersionRejectsInsufficientVersions(int version) {
        var errOutput = new ByteArrayOutputStream();
        var err = new PrintStream(errOutput);

        boolean result = Bootstrap.checkVersion(version, err);

        assertThat(result).isFalse();
        String output = errOutput.toString(StandardCharsets.UTF_8);
        assertThat(output).contains("maven-mcp requires Java 21+");
        assertThat(output).contains("Detected: Java " + version);
        assertThat(output).contains("https://adoptium.net/");
    }

    @ParameterizedTest
    @ValueSource(ints = {21, 22, 25})
    void checkVersionAcceptsSufficientVersions(int version) {
        var errOutput = new ByteArrayOutputStream();
        var err = new PrintStream(errOutput);

        boolean result = Bootstrap.checkVersion(version, err);

        assertThat(result).isTrue();
        assertThat(errOutput.toString(StandardCharsets.UTF_8)).isEmpty();
    }

    @Test
    void checkVersionErrorMessageHasExactThreeLines() {
        var errOutput = new ByteArrayOutputStream();
        var err = new PrintStream(errOutput);

        Bootstrap.checkVersion(17, err);

        String[] lines = errOutput.toString(StandardCharsets.UTF_8).trim().split("\n");
        assertThat(lines).hasSize(3);
        assertThat(lines[0]).isEqualTo("maven-mcp requires Java 21+");
        assertThat(lines[1]).isEqualTo("Detected: Java 17");
        assertThat(lines[2]).isEqualTo("Install Java 21+: https://adoptium.net/");
    }
}
