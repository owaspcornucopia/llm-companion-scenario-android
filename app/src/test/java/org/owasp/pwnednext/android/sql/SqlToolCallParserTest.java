package org.owasp.pwnednext.android.sql;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public final class SqlToolCallParserTest {
    private final SqlToolCallParser parser = new SqlToolCallParser();

    @Test
    public void parsesRawSelect() {
        assertEquals("SELECT * FROM transactions", parser.parse(" SELECT * FROM transactions "));
    }

    @Test
    public void parsesFencedJson() {
        assertEquals(
                "SELECT * FROM transactions WHERE transaction_id = 'TX-1002'",
                parser.parse("```json\n{\"sql\":\"SELECT * FROM transactions WHERE transaction_id = 'TX-1002'\"}\n```"));
    }

    @Test
    public void parsesNestedJsonString() {
        assertEquals(
                "SELECT * FROM transactions",
                parser.parse("{\"arguments\":\"{\\\"sql\\\":\\\"SELECT * FROM transactions\\\"}\"}"));
    }

    @Test
    public void parsesWithAndPragmaStatements() {
        assertEquals("WITH matches AS (SELECT 1) SELECT * FROM matches", parser.parse(
                "WITH matches AS (SELECT 1) SELECT * FROM matches"));
        assertEquals("PRAGMA table_info(transactions)", parser.parse("PRAGMA table_info(transactions)"));
    }

    @Test
    public void parsesEscapedJsonCharacters() {
        assertEquals(
                "SELECT 'line\nbreak'",
                parser.parse("{\"sql\":\"SELECT 'line\\nbreak'\"}"));
    }

    @Test
    public void parsesEverySupportedJsonEscape() {
        String sql = "SELECT \"x\" \\ / \b \f \n \r \t A";
        assertEquals(sql, parser.parse("{\"sql\":" + JsonEscaper.quote(sql) + "}"));
    }

    @Test
    public void rejectsBlankAndUnsupportedOutput() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
        assertThrows(IllegalArgumentException.class, () -> parser.parse(" "));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("{\"answer\":\"no query\"}"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("{\"sql\":\"\"}"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("```sql"));
    }

    @Test
    public void rejectsMalformedUnicodeEscape() {
        String malformedUnicode = "{\"sql\":\"SELECT " + "\\" + "u12ZZ\"}";
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(malformedUnicode));
        String shortUnicode = "{\"sql\":\"SELECT " + "\\" + "u12\"}";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(shortUnicode));
    }

    @Test
    public void rejectsUnterminatedEscape() {
        String unterminatedJson = "{\"sql\":\"SELECT " + "\\";
        assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(unterminatedJson));
    }
}
