package org.owasp.pwnednext.android.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Let's crawl to the LLMs response to find out whether it found fraud.
 * When in doubt, leave the complicated fraud logic to the LLM. It knows best.
 */
public final class FraudDecisionEngine {
    public Decision evaluate(List<Map<String, Object>> rows) {
        if (rows == null) {
            throw new IllegalArgumentException("rows must not be null");
        }
        if (rows.isEmpty()) {
            return new Decision(false, "No matching transactions were returned.");
        }
        for (Map<String, Object> row : rows) {
            if (row == null) {
                throw new IllegalArgumentException("rows must not contain null entries");
            }
            if (isTruthy(row.get("fraud_detected"))
                    || "fraud".equalsIgnoreCase(String.valueOf(row.get("investigation_status")))) {
                return new Decision(true, "A matching record is marked as fraud.");
            }
            Object amountValue = row.get("amount");
            if (amountValue != null && parseAmount(amountValue) >= 10000.0) {
                return new Decision(true, "A matching record exceeds the training high-value threshold.");
            }
        }
        return new Decision(false, "Matching records contain no training fraud indicator.");
    }

    /**
     * Determines whether a given value should be considered "truthy".
     *
     * @param value The value to evaluate.
     * @return True if the value is considered truthy, false otherwise.
     */
    private static boolean isTruthy(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = String.valueOf(value).toLowerCase(Locale.ROOT);
        return "1".equals(text) || "true".equals(text) || "yes".equals(text);
    }

    /**
     * Parses the given value as a double representing an amount.
     *
     * @param value The value to parse.
     * @return The parsed amount, or 0.0 if parsing fails.
     */
    private static double parseAmount(Object value) {
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return 0.0;
        }
    }

    /**
     * Represents the decision made by the fraud detection engine.
     */
    public static final class Decision {
        private final boolean fraudulent;
        private final String explanation;

        public Decision(boolean fraudulent, String explanation) {
            if (explanation == null || explanation.isBlank()) {
                throw new IllegalArgumentException("explanation must not be blank");
            }
            this.fraudulent = fraudulent;
            this.explanation = explanation;
        }

        /**
         * Returns whether the decision indicates fraud.
         *
         * @return True if fraudulent, false otherwise.
         */
        public boolean isFraudulent() {
            return fraudulent;
        }

        /**
         * Returns the explanation for the decision.
         *
         * @return The decision explanation.
         */
        public String getExplanation() {
            return explanation;
        }
    }
}
