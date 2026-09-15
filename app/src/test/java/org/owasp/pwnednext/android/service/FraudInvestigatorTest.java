package org.owasp.pwnednext.android.service;

import org.owasp.pwnednext.android.model.InvestigationResult;
import org.owasp.pwnednext.android.model.ModelResponse;
import org.owasp.pwnednext.android.sql.SqlModel;
import org.owasp.pwnednext.android.sql.SqlToolCallParser;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public final class FraudInvestigatorTest {
    @Test
    public void runsModelParserExecutorAndDecisionInOrder() throws IOException, SQLException {
        SqlModel model = question -> new ModelResponse(
                "{\"sql\":\"SELECT * FROM transactions WHERE transaction_id = 'TX-1002'\"}",
                "test-model");
        SqlExecutor executor = sql -> List.of(Map.of(
                "transaction_id", "TX-1002",
                "fraud_detected", 1));

        InvestigationResult result = new FraudInvestigator(
                model,
                new SqlToolCallParser(),
                executor,
                new FraudDecisionEngine()).investigate("Is TX-1002 fraudulent?");

        assertTrue(result.getProvider().startsWith("test-model / offline-summary"));
        assertEquals("SELECT * FROM transactions WHERE transaction_id = 'TX-1002'", result.getSql());
        assertTrue(result.isFraudulent());
        assertTrue(result.getModelAnswer().contains("rows"));
        assertEquals(1, result.getRows().size());
    }

    @Test
    public void propagatesExecutorFailure() {
        SqlModel model = question -> new ModelResponse("{\"sql\":\"SELECT 1\"}", "test-model");
        SqlExecutor executor = sql -> {
            throw new SQLException("database unavailable");
        };
        FraudInvestigator investigator = new FraudInvestigator(
                model,
                new SqlToolCallParser(),
                executor,
                new FraudDecisionEngine());

        assertThrows(SQLException.class, () -> investigator.investigate("question"));
    }

    @Test
    public void rejectsBlankQuestionAndMissingCollaborators() {
        SqlModel model = question -> new ModelResponse("{\"sql\":\"SELECT 1\"}", "test-model");
        FraudDecisionEngine engine = new FraudDecisionEngine();
        assertThrows(IllegalArgumentException.class, () -> new FraudInvestigator(
                model, new SqlToolCallParser(), sql -> List.of(), engine).investigate(""));
        assertThrows(IllegalArgumentException.class, () -> new FraudInvestigator(
                null, new SqlToolCallParser(), sql -> List.of(), engine));
        assertThrows(IllegalArgumentException.class, () -> new FraudInvestigator(
                model, null, sql -> List.of(), engine));
        assertThrows(IllegalArgumentException.class, () -> new FraudInvestigator(
                model, new SqlToolCallParser(), null, engine));
        assertThrows(IllegalArgumentException.class, () -> new FraudInvestigator(
                model, new SqlToolCallParser(), sql -> List.of(), null));
    }
}
