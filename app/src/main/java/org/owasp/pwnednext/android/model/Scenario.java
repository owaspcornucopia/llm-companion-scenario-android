package org.owasp.pwnednext.android.model;

/**
 * Stores card data instead of another screen, because even this overconfident app can reuse a list.
 */
public final class Scenario {
    private final String code;
    private final String category;
    private final String title;
    private final boolean applicable;
    private final boolean implemented;
    private final String explanation;

    public Scenario(
            String code,
            String category,
            String title,
            boolean applicable,
            boolean implemented,
            String explanation) {
        this.code = requireText(code, "code");
        this.category = requireText(category, "category");
        this.title = requireText(title, "title");
        this.explanation = requireText(explanation, "explanation");
        this.applicable = applicable;
        this.implemented = implemented;
    }

    /**
     * Returns the code of the scenario.
     *
     * @return The scenario code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the category of the scenario.
     *
     * @return The scenario category.
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns the title of the scenario.
     *
     * @return The scenario title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Indicates whether the scenario is applicable.
     *
     * @return True if applicable, false otherwise.
     */
    public boolean isApplicable() {
        return applicable;
    }

    /**
     * Indicates whether the scenario has been implemented.
     *
     * @return True if implemented, false otherwise.
     */
    public boolean isImplemented() {
        return implemented;
    }

    /**
     * Returns the explanation for the scenario.
     *
     * @return The scenario explanation.
     */
    public String getExplanation() {
        return explanation;
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
