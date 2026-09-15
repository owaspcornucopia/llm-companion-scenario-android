package org.owasp.pwnednext.android.sql;

import org.owasp.pwnednext.android.model.ModelResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Provides a simple interface for generating and summarizing SQL queries using a model.
 * This interface abstracts the underlying model implementation, allowing for different models to be used interchangeably.
 * This allows for flexibility in switching between different SQL model implementations without changing the code that uses this interface.
 */
public interface SqlModel {
    ModelResponse generate(String question) throws IOException;

    default ModelResponse summarize(
            String question,
            String sql,
            List<Map<String, Object>> rows) throws IOException {
        return new ModelResponse(
                "The model received " + rows.size() + " rows for the final answer.",
                "offline-summary");
    }
}
