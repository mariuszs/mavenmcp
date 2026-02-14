package io.github.mavenmcp.tool;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mavenmcp.maven.MavenExecutionResult;
import io.github.mavenmcp.model.BuildResult;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Shared utilities for MCP tool handlers.
 */
final class ToolUtils {

    private ToolUtils() {
    }

    /**
     * Extract the "args" parameter from the tool call arguments.
     *
     * @param params the tool call parameters map
     * @return list of extra Maven arguments, empty if not provided
     */
    @SuppressWarnings("unchecked")
    static List<String> extractArgs(Map<String, Object> params) {
        Object argsObj = params.get("args");
        if (argsObj instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .toList();
        }
        return Collections.emptyList();
    }

    /**
     * Extract a boolean parameter from tool call arguments.
     *
     * @param params       the tool call parameters map
     * @param key          the parameter key
     * @param defaultValue value to return if key is absent or not a boolean
     * @return the boolean value or default
     */
    static boolean extractBoolean(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value instanceof Boolean b) {
            return b;
        }
        return defaultValue;
    }

    /**
     * Extract an integer parameter from tool call arguments.
     *
     * @param params       the tool call parameters map
     * @param key          the parameter key
     * @param defaultValue value to return if key is absent or not a number
     * @return the integer value or default
     */
    static int extractInt(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number num) {
            return num.intValue();
        }
        return defaultValue;
    }

    /**
     * Build a timeout BuildResult and return as error CallToolResult.
     *
     * @param execResult   the timed-out execution result
     * @param objectMapper Jackson mapper for JSON serialization
     * @return a CallToolResult with isError=true and TIMEOUT status
     * @throws JsonProcessingException if serialization fails
     */
    static CallToolResult handleTimeout(MavenExecutionResult execResult, ObjectMapper objectMapper)
            throws JsonProcessingException {
        var buildResult = new BuildResult(
                BuildResult.TIMEOUT, execResult.duration(),
                null, null, null, null, null, execResult.stdout());
        String json = objectMapper.writeValueAsString(buildResult);
        return new CallToolResult(List.of(new TextContent(json)), true);
    }

    /**
     * Serialize a BuildResult to a CallToolResult.
     *
     * @param buildResult  the build result to serialize
     * @param objectMapper Jackson mapper for JSON serialization
     * @return a CallToolResult with isError=false
     * @throws JsonProcessingException if serialization fails
     */
    static CallToolResult toResult(BuildResult buildResult, ObjectMapper objectMapper)
            throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(buildResult);
        return new CallToolResult(List.of(new TextContent(json)), false);
    }
}
