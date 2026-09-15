package org.owasp.pwnednext.android.model;

/**
 * Let's keep the question from the user and the response from TinyLLama together.
 * The testers need to understand how good this model is performing.
 */
public final class ModelResponse {
    private final String output;
    private final String provider;

    public ModelResponse(String output, String provider) {
        if (output == null || output.isBlank()) {
            throw new IllegalArgumentException("Model output must not be blank");
        }
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("Model provider must not be blank");
        }
        this.output = output;
        this.provider = provider;
    }

    /**
     * Returns the model's output.
     *
     * @return The model output.
     */
    public String getOutput() {
        return output;
    }

    /**
     * Returns the provider of the model's output.
     *
     * @return The model provider.
     */
    public String getProvider() {
        return provider;
    }
}
