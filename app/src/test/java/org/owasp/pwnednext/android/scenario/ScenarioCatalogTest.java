package org.owasp.pwnednext.android.scenario;

import org.owasp.pwnednext.android.model.Scenario;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ScenarioCatalogTest {
    @Test
    public void containsImplementedAndExplicitlyExcludedCards() {
        List<Scenario> scenarios = ScenarioCatalog.all();
        Set<String> codes = new HashSet<>();
        boolean hasImplemented = false;
        boolean hasExcluded = false;
        for (Scenario scenario : scenarios) {
            assertTrue(codes.add(scenario.getCode()));
            if (scenario.isImplemented()) {
                hasImplemented = true;
            }
            if (!scenario.isApplicable()) {
                hasExcluded = true;
                assertFalse(scenario.isImplemented());
            }
        }
        assertTrue(hasImplemented);
        assertTrue(hasExcluded);
        assertEquals(80 + 12 + 3, scenarios.size());
        for (String code : new String[] {
                "PC3", "PC4", "PC5", "PC6", "PC7", "PC8", "PC9", "PCQ",
                "AA2", "AA7", "AA8", "AA9", "AAQ",
                "NS3", "NS5", "NS6", "NS7", "NS8", "NS9",
                "RS3", "RS4", "RS5", "RS7", "RS8", "RS9", "RSX", "RSJ", "RSQ",
                "CRM2", "CRM3", "CRM4", "CRM6", "CRM7", "CRM9", "CRMX",
                "CM8", "CMX"}) {
            Scenario selected = find(scenarios, code);
            assertTrue(selected.isApplicable());
            assertTrue(selected.isImplemented());
        }
        for (String code : new String[] {
                "PCX", "PCJ", "PCK", "PCA",
                "AA3", "AA4", "AA5", "AA6", "AAX", "AAJ", "AAK", "AAA",
                "NSJ", "NSX", "NSQ", "NSK", "NSA",
                "RS6", "RSK", "RSA",
                "CRM5", "CRM8", "CRMJ", "CRMQ", "CRMK", "CRMA",
                "CM2", "CM3", "CM4", "CM5", "CM6", "CM7", "CM9",
                "CMJ", "CMQ", "CMK", "CMA", "JOAM", "JOBM"}) {
            Scenario excluded = find(scenarios, code);
            assertFalse(excluded.isApplicable());
            assertFalse(excluded.isImplemented());
        }
    }

    @Test
    public void returnsAnUnmodifiableList() {
        assertThrowsUnsupported(ScenarioCatalog.all());
    }

    @Test
    public void applicableCardSetMatchesDocumentedScenario() {
        Set<String> expected = Set.of(
                "PC2", "PC3", "PC4", "PC5", "PC6", "PC7", "PC8", "PC9", "PCQ",
                "AA2", "AA7", "AA8", "AA9", "AAQ",
                "NS2", "NS3", "NS4", "NS5", "NS6", "NS7", "NS8", "NS9",
                "RS2", "RS3", "RS4", "RS5", "RS7", "RS8", "RS9", "RSJ", "RSQ", "RSX",
                "CRM2", "CRM3", "CRM4", "CRM6", "CRM7", "CRM9", "CRMX",
                "CM8", "CMX",
                "LLM2", "LLM3", "LLM4", "LLM5", "LLM7", "LLM8",
                "LLM9", "LLMJ", "LLMK", "LLMQ", "LLMX");
        Set<String> actual = new HashSet<>();
        for (Scenario scenario : ScenarioCatalog.all()) {
            if (scenario.isApplicable()) {
                actual.add(scenario.getCode());
            }
        }
        assertEquals(expected, actual);
    }

    private static void assertThrowsUnsupported(List<Scenario> scenarios) {
        org.junit.Assert.assertThrows(
                UnsupportedOperationException.class,
                () -> scenarios.clear());
    }

    private static Scenario find(List<Scenario> scenarios, String code) {
        for (Scenario scenario : scenarios) {
            if (scenario.getCode().equals(code)) {
                return scenario;
            }
        }
        throw new AssertionError("Missing scenario " + code);
    }
}
