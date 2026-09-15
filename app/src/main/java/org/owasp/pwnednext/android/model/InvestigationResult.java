package org.owasp.pwnednext.android.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixing so that the app shows everything the app does to the testers.
 * Testers may be technical incompetent, but they can be useful for doing the boring stuff.
 */
public final class InvestigationResult {
    private final String question;
    private final String sql;
    private final String provider;
    private final boolean fraudulent;
    private final String explanation;
    private final String modelAnswer;
    private final List<Map<String, Object>> rows;

    public InvestigationResult(
            String question,
            String sql,
            String provider,
            boolean fraudulent,
            String explanation,
            String modelAnswer,
            List<Map<String, Object>> rows) {
        this.question = requireText(question, "question");
        this.sql = requireText(sql, "sql");
        this.provider = requireText(provider, "provider");
        this.explanation = requireText(explanation, "explanation");
        this.modelAnswer = requireText(modelAnswer, "modelAnswer");
        if (rows == null) {
            throw new IllegalArgumentException("rows must not be null");
        }
        List<Map<String, Object>> copiedRows = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (row == null) {
                throw new IllegalArgumentException("rows must not contain null entries");
            }
            copiedRows.add(Collections.unmodifiableMap(new LinkedHashMap<>(row)));
        }
        this.rows = Collections.unmodifiableList(copiedRows);
        this.fraudulent = fraudulent;
    }

    /**
     * Returns the question that prompted this investigation.
     *
     * @return The investigation question.
     */
    public String getQuestion() {
        return question;
    }

    /**
     * Returns the SQL query associated with this investigation.
     *
     * @return The SQL query.
     */
    public String getSql() {
        return sql;
    }

    /**
     * Returns the provider that handled this investigation.
     *
     * @return The provider.
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Indicates whether the investigation determined the content to be fraudulent.
     *
     * @return True if fraudulent, false otherwise.
     */
    public boolean isFraudulent() {
        return fraudulent;
    }

    /**
     * Returns the explanation for the investigation's findings.
     *
     * @return The explanation.
     */
    public String getExplanation() {
        return explanation;
    }

    /**
     * Returns the model's answer for the investigation.
     *
     * @return The model answer.
     */
    public String getModelAnswer() {
        return modelAnswer;
    }

    /**
     * Returns the rows of data retrieved during the investigation.
     *
     * @return The list of rows.
     */
    public List<Map<String, Object>> getRows() {
        return rows;
    }

    /**
     * Ensures that a given text value is not null or blank.
     *
     * @param value The text value to check.
     * @param field The name of the field being checked.
     * @return The validated text value.
     * @throws IllegalArgumentException If the value is null or blank.
     */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
