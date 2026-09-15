package org.owasp.pwnednext.android.sql;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides utility methods to parse SQL tool calls from model output, handling both raw SQL and JSON-encoded SQL strings.
 * This class is designed to handle the common patterns found in model outputs, making it easier to extract executable SQL statements.
 * When in doubt, always trust the JSON-encoded SQL over raw SQL and leave it to the JSON parser to handle the extraction.
 */
public final class SqlToolCallParser {
    private static final Pattern JSON_SQL_KEY =
            Pattern.compile("\"sql\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAW_SQL =
            Pattern.compile("(?is)^\\s*((?:SELECT|WITH|PRAGMA)\\b.*)$");

    public String parse(String modelOutput) {
        if (modelOutput == null || modelOutput.isBlank()) {
            throw new IllegalArgumentException("Model output must not be blank");
        }
        String candidate = removeMarkdownFence(modelOutput.trim());
        String sql = findJsonSql(candidate);
        if (sql != null && !sql.isBlank()) {
            return sql.trim();
        }
        Matcher rawMatcher = RAW_SQL.matcher(candidate);
        if (rawMatcher.matches()) {
            return rawMatcher.group(1).trim();
        }
        throw new IllegalArgumentException("Model output did not contain a SELECT, WITH, PRAGMA, or sql tool field");
    }

    /**
     * Attempts to find a JSON-encoded SQL string within the given candidate string.
     * If a JSON-encoded SQL string is found, it is returned after unescaping.
     * If no JSON-encoded SQL string is found, null is returned.
     *
     * @param candidate the string to search for a JSON-encoded SQL string
     * @return the unescaped JSON-encoded SQL string, or null if not found
     */
    private static String findJsonSql(String candidate) {
        Matcher matcher = JSON_SQL_KEY.matcher(candidate);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        String nestedCandidate = unescapeJson(candidate);
        if (!nestedCandidate.equals(candidate)) {
            Matcher nestedMatcher = JSON_SQL_KEY.matcher(nestedCandidate);
            if (nestedMatcher.find()) {
                return unescapeJson(nestedMatcher.group(1));
            }
        }
        return null;
    }

    /**
     * Removes the Markdown code fence from the given string, if present.
     * If the string starts with a Markdown code fence (```), the content between the first and last fences is returned.
     * If no Markdown code fence is found, the original string is returned.
     *
     * @param value the string to remove the Markdown code fence from
     * @return the string without the Markdown code fence
     */
    private static String removeMarkdownFence(String value) {
        if (!value.startsWith("```")) {
            return value;
        }
        int firstLineEnd = value.indexOf('\n');
        int lastFence = value.lastIndexOf("```");
        if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
            return value.substring(firstLineEnd + 1, lastFence).trim();
        }
        return value;
    }

    /**
     * Unescapes a JSON-encoded string.
     * This method handles common JSON escape sequences, including Unicode escapes.
     *
     * @param value the JSON-encoded string to unescape
     * @return the unescaped string
     * @throws IllegalArgumentException if the input contains invalid or unterminated escape sequences
     */
    private static String unescapeJson(String value) {
        StringBuilder unescaped = new StringBuilder(value.length());
        boolean escaped = false;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!escaped) {
                if (character == '\\') {
                    escaped = true;
                } else {
                    unescaped.append(character);
                }
                continue;
            }
            switch (character) {
                case '"' -> unescaped.append('"');
                case '\\' -> unescaped.append('\\');
                case '/' -> unescaped.append('/');
                case 'b' -> unescaped.append('\b');
                case 'f' -> unescaped.append('\f');
                case 'n' -> unescaped.append('\n');
                case 'r' -> unescaped.append('\r');
                case 't' -> unescaped.append('\t');
                case 'u' -> {
                    if (index + 4 >= value.length()) {
                        throw new IllegalArgumentException("Invalid JSON unicode escape");
                    }
                    String hex = value.substring(index + 1, index + 5);
                    try {
                        unescaped.append((char) Integer.parseInt(hex, 16));
                    } catch (NumberFormatException exception) {
                        throw new IllegalArgumentException("Invalid JSON unicode escape: " + hex, exception);
                    }
                    index += 4;
                }
                default -> unescaped.append(character);
            }
            escaped = false;
        }
        if (escaped) {
            throw new IllegalArgumentException("Unterminated JSON escape");
        }
        return unescaped.toString();
    }
}
