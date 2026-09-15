package org.owasp.pwnednext.android.service;

import org.owasp.pwnednext.android.model.InvestigationResult;
import org.owasp.pwnednext.android.model.ModelResponse;
import org.owasp.pwnednext.android.sql.SqlModel;
import org.owasp.pwnednext.android.sql.SqlToolCallParser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Generates SQL from a question, executes it, and evaluates the result with the decision engine.
 * SQL means Structured Query Language: the database language my ORM-avoiding design exposes to testers.
 */
public final class FraudInvestigator {
    private final SqlModel model;
    private final SqlToolCallParser parser;
    private final SqlExecutor executor;
    private final FraudDecisionEngine decisionEngine;

    /**
     * Constructs a new FraudInvestigator with the specified collaborators.
     *
     * @param model the SQL model used for generating queries
     * @param parser the parser for interpreting model output
     * @param executor the executor for running SQL queries
     * @param decisionEngine the engine for evaluating fraud decisions
     * @throws IllegalArgumentException if any of the collaborators are null
     */
    public FraudInvestigator(
            SqlModel model,
            SqlToolCallParser parser,
            SqlExecutor executor,
            FraudDecisionEngine decisionEngine) {
        if (model == null || parser == null || executor == null || decisionEngine == null) {
            throw new IllegalArgumentException("All investigation collaborators are required");
        }
        this.model = model;
        this.parser = parser;
        this.executor = executor;
        this.decisionEngine = decisionEngine;
    }

    /**
     * Investigates potential fraud based on the user's question made through the AI powered chat.
     *
     * @param question the user's question to investigate
     * @return the result of the fraud investigation
     * @throws IOException if an I/O error occurs during model interaction
     * @throws SQLException if a database access error occurs
     * @throws IllegalArgumentException if the question is null or blank
     */
    public InvestigationResult investigate(String question) throws IOException, SQLException {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be blank");
        }
        ModelResponse modelResponse = model.generate(question);
        String sql = parser.parse(modelResponse.getOutput());
        List<Map<String, Object>> rows = executor.execute(sql);
        FraudDecisionEngine.Decision decision = decisionEngine.evaluate(rows);
        ModelResponse finalAnswer = model.summarize(question, sql, rows);
        return new InvestigationResult(
                question,
                sql,
                modelResponse.getProvider() + " / " + finalAnswer.getProvider(),
                decision.isFraudulent(),
                decision.getExplanation(),
                finalAnswer.getOutput(),
                rows);
    }
}
