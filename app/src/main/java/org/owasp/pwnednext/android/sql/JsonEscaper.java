package org.owasp.pwnednext.android.sql;

/**
 * Escapes JSON strings without adding a dependency for one small utility.
 */
public final class JsonEscaper {
    private JsonEscaper() {
    }

    public static String quote(String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        StringBuilder escaped = new StringBuilder(value.length() + 2);
        escaped.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        escaped.append('"');
        return escaped.toString();
    }
}
