package io.github.mavenmcp;

import java.io.PrintStream;
import java.lang.reflect.Method;

/**
 * Bootstrap entry point compiled at Java 11 bytecode level.
 * Validates JVM version before loading the main application
 * to provide a clear error message instead of UnsupportedClassVersionError.
 */
public class Bootstrap {

    static final int REQUIRED_JAVA_VERSION = 21;

    public static void main(String[] args) {
        int currentVersion = Runtime.version().feature();
        if (!checkVersion(currentVersion, System.err)) {
            System.exit(1);
        }

        try {
            Class<?> serverClass = Class.forName("io.github.mavenmcp.MavenMcpServer");
            Method mainMethod = serverClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) args);
        } catch (Exception e) {
            System.err.println("Failed to start maven-mcp: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Checks if the given Java version meets the minimum requirement.
     * On failure, prints a human-readable error to the provided stream.
     *
     * @param detectedVersion the JVM feature version
     * @param err stream for error output
     * @return true if version is sufficient, false otherwise
     */
    static boolean checkVersion(int detectedVersion, PrintStream err) {
        if (detectedVersion < REQUIRED_JAVA_VERSION) {
            err.println("maven-mcp requires Java " + REQUIRED_JAVA_VERSION + "+");
            err.println("Detected: Java " + detectedVersion);
            err.println("Install Java " + REQUIRED_JAVA_VERSION + "+: https://adoptium.net/");
            return false;
        }
        return true;
    }
}
