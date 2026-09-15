package org.owasp.pwnednext.android.service;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public final class FraudDecisionEngineTest {
    private final FraudDecisionEngine engine = new FraudDecisionEngine();

    @Test
    public void returnsNoFraudForNoRows() {
        FraudDecisionEngine.Decision decision = engine.evaluate(List.of());
        assertFalse(decision.isFraudulent());
        assertTrue(decision.getExplanation().contains("No matching"));
    }

    @Test
    public void detectsBooleanAndTextFraudFlags() {
        assertTrue(engine.evaluate(List.of(Map.of("fraud_detected", true))).isFraudulent());
        assertTrue(engine.evaluate(List.of(Map.of("fraud_detected", "yes"))).isFraudulent());
        assertTrue(engine.evaluate(List.of(Map.of("investigation_status", "FRAUD"))).isFraudulent());
    }

    @Test
    public void detectsHighValueTransaction() {
        FraudDecisionEngine.Decision decision =
                engine.evaluate(List.of(Map.of("amount", "10000.01")));
        assertTrue(decision.isFraudulent());
        assertTrue(decision.getExplanation().contains("high-value"));
    }

    @Test
    public void ignoresMalformedAmountAndClearRows() {
        FraudDecisionEngine.Decision decision = engine.evaluate(List.of(
                Map.of("amount", "not-a-number", "fraud_detected", "no"),
                Map.of("amount", 9999, "investigation_status", "clear")));
        assertFalse(decision.isFraudulent());
        assertTrue(decision.getExplanation().contains("no training fraud"));
    }

    @Test
    public void rejectsNullRows() {
        assertThrows(IllegalArgumentException.class, () -> engine.evaluate(null));
        assertThrows(IllegalArgumentException.class, () -> engine.evaluate(java.util.Arrays.asList((Map<String, Object>) null)));
    }

    @Test
    public void rejectsBlankDecisionExplanation() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new FraudDecisionEngine.Decision(false, ""));
    }
}
