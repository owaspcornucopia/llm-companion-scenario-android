package org.owasp.pwnednext.android.sql;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class JsonEscaperTest {
    @Test
    public void quotesControlCharactersAndJsonDelimiters() {
        assertEquals(
                "\"quote: \\\" slash: \\\\ line\\n tab\\t\"",
                JsonEscaper.quote("quote: \" slash: \\ line\n tab\t"));
    }

    @Test
    public void quotesLowAsciiCharactersAsUnicode() {
        assertEquals("\"\\u0001\"", JsonEscaper.quote("\u0001"));
    }

    @Test
    public void quotesTheRemainingJsonControlCharacters() {
        assertEquals("\"\\b\\f\\r\"", JsonEscaper.quote("\b\f\r"));
    }

    @Test
    public void rejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> JsonEscaper.quote(null));
    }
}
