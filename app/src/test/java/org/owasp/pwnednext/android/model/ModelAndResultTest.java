package org.owasp.pwnednext.android.model;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class ModelAndResultTest {
    @Test
    public void modelResponseExposesValidatedValues() {
        ModelResponse response = new ModelResponse("output", "provider");
        assertEquals("output", response.getOutput());
        assertEquals("provider", response.getProvider());
        assertThrows(IllegalArgumentException.class, () -> new ModelResponse("", "provider"));
        assertThrows(IllegalArgumentException.class, () -> new ModelResponse("output", ""));
    }

    @Test
    public void investigationResultCopiesRowsAndExposesValues() {
        java.util.HashMap<String, Object> row = new java.util.HashMap<>();
        row.put("transaction_id", "TX-1001");
        InvestigationResult result = new InvestigationResult(
                "question",
                "SELECT 1",
                "provider",
                false,
                "clear",
                "model answer",
                List.of(row));

        row.put("changed", true);
        assertEquals("question", result.getQuestion());
        assertEquals("SELECT 1", result.getSql());
        assertEquals("provider", result.getProvider());
        assertTrue(!result.isFraudulent());
        assertEquals("clear", result.getExplanation());
        assertEquals("model answer", result.getModelAnswer());
        assertEquals(1, result.getRows().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> result.getRows().get(0).put("blocked", true));
        assertThrows(IllegalArgumentException.class, () -> new InvestigationResult(
                "", "SELECT 1", "provider", false, "clear", "answer", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new InvestigationResult(
                "question", "", "provider", false, "clear", "answer", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new InvestigationResult(
                "question", "SELECT 1", "provider", false, "clear", "answer", null));
        assertThrows(IllegalArgumentException.class, () -> new InvestigationResult(
                "question", "SELECT 1", "", false, "clear", "answer", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new InvestigationResult(
                "question", "SELECT 1", "provider", false, "", "answer", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new InvestigationResult(
                "question", "SELECT 1", "provider", false, "clear", "", List.of()));
        assertThrows(IllegalArgumentException.class, () -> new InvestigationResult(
                "question",
                "SELECT 1",
                "provider",
                false,
                "clear",
                "answer",
                java.util.Arrays.asList((Map<String, Object>) null)));
    }

}
