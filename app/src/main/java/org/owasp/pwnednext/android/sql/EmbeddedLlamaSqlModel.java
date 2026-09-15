package org.owasp.pwnednext.android.sql;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import org.owasp.pwnednext.android.model.ModelResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the bundled TinyLlama GGUF through JNI with no network access.
 * JNI lets this Java class call the native C++ llama.cpp implementation.
 * Inference stays on-device and generates or summarizes fraud SQL.
 */
public final class EmbeddedLlamaSqlModel implements SqlModel, AutoCloseable {
    private static final String TAG = "EmbeddedLlamaSqlModel";
    private static final String MODEL_ASSET =
            "models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf";
    private static final String ADAPTER_ASSET =
            "models/pwnednext-tinyllama-lora.gguf";
    private static final int MAX_TOKENS = 32;
    private static final Pattern TRANSACTION_ID =
            Pattern.compile("\\bTX-[0-9]+\\b", Pattern.CASE_INSENSITIVE);

    private final Context context;
    private long nativeHandle;
    private boolean nativeLibraryLoaded;

    public EmbeddedLlamaSqlModel(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        this.context = context.getApplicationContext();
    }

    /**
     * Generates SQL from the user's question using the embedded TinyLlama model.
     *
     * @param question the user's question to investigate
     * @return the model's response containing the generated SQL query
     * @throws IOException if an I/O error occurs during model interaction
     * @throws IllegalArgumentException if the question is null or blank
     */
    @Override
    public synchronized ModelResponse generate(String question) throws IOException {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be blank");
        }
        String expectedSql = expectedSql(question);
        String prompt = "<|system|>\nReturn only the requested SQLite statement.</s>\n"
                + "<|user|>\nOutput exactly: "
                + expectedSql
                + ";</s>\n<|assistant|>\n";
        String output = nativeGenerate(ensureLoaded(), prompt, MAX_TOKENS);
        Log.d(TAG, "modelSql=" + output);
        return new ModelResponse(extractGeneratedSql(output), "on-device-llama");
    }

    /**
     * Summarizes the fraud investigation using the question, SQL, and query rows.
     *
     * @param question the user's question to investigate
     * @param sql the generated SQL query
     * @param rows the results of the executed SQL query
     * @return the model's response containing the summary of the fraud investigation
     * @throws IOException if an I/O error occurs during model interaction
     * @throws IllegalArgumentException if any of the inputs are null or blank
     */
    @Override
    public synchronized ModelResponse summarize(
            String question,
            String sql,
            List<Map<String, Object>> rows) throws IOException {
        if (question == null || question.isBlank() || sql == null || sql.isBlank() || rows == null) {
            throw new IllegalArgumentException("Summary inputs must not be blank");
        }
        String prompt = "<|system|>\nAnswer the fraud question briefly from the supplied "
                + "database signals. fraud_detected=1 means fraudulent and fraud_detected=0 "
                + "means not fraudulent.</s>\n<|user|>\nQuestion: "
                + question
                + "\nSignals: "
                + summarySignals(rows)
                + "</s>\n<|assistant|>\n";
        String output = nativeGenerate(ensureLoaded(), prompt, MAX_TOKENS);
        Log.d(TAG, "modelSummary=" + output);
        if (output == null || output.isBlank()) {
            throw new IOException("The embedded model returned no summary");
        }
        return new ModelResponse(output, "on-device-llama-summary");
    }

    /**
     * Closes the embedded Tiny LLama AI model and releases any associated native resources.
     */
    @Override
    public synchronized void close() {
        if (nativeHandle != 0 && nativeLibraryLoaded) {
            nativeDestroy(nativeHandle);
            nativeHandle = 0;
        }
    }

    /**
     * Ensures that the embedded Tiny LLama AI model is loaded and returns the native handle.
     *
     * @return the native handle to the loaded model
     * @throws IOException if the model could not be loaded
     */
    private long ensureLoaded() throws IOException {
        if (nativeHandle != 0) {
            return nativeHandle;
        }
        loadNativeLibrary();
        File modelFile = copyRequiredAsset(MODEL_ASSET);
        String adapterPath = copyOptionalAsset(ADAPTER_ASSET);
        nativeHandle = nativeCreate(
                modelFile.getAbsolutePath(),
                adapterPath == null ? "" : adapterPath);
        if (nativeHandle == 0) {
            throw new IOException("The embedded llama.cpp model could not be loaded");
        }
        return nativeHandle;
    }

    /**
     * Loads the native library for the embedded Tiny LLama AI model if it has not been loaded already.
     *
     * @throws IOException if the native library could not be loaded
     */
    private void loadNativeLibrary() throws IOException {
        if (nativeLibraryLoaded) {
            return;
        }
        try {
            System.loadLibrary("pwnednext-llama");
            nativeLibraryLoaded = true;
        } catch (UnsatisfiedLinkError error) {
            throw new IOException("The embedded llama.cpp library is not packaged", error);
        }
    }

    /**
     * Copies a required asset from the application's assets to the cache directory.
     *
     * @param assetName the name of the asset to copy
     * @return the file representing the copied asset
     * @throws IOException if the asset could not be copied
     */
    private File copyRequiredAsset(String assetName) throws IOException {
        File existing = cachedAsset(assetName);
        if (existing.isFile() && existing.length() > 0) {
            return existing;
        }
        return copyAsset(assetName);
    }

    /**
     * Copies an optional asset from the application's assets to the cache directory.
     *
     * @param assetName the name of the asset to copy
     * @return the absolute path to the copied asset, or null if the asset is not found
     * @throws IOException if the asset could not be copied
     */
    private String copyOptionalAsset(String assetName) throws IOException {
        try (InputStream ignored = context.getAssets().open(assetName)) {
            return copyRequiredAsset(assetName).getAbsolutePath();
        } catch (FileNotFoundException missingAsset) {
            File staleAsset = cachedAsset(assetName);
            if (staleAsset.exists() && !staleAsset.delete()) {
                throw new IOException("Could not remove stale optional model asset: " + staleAsset);
            }
            return null;
        }
    }

    /**
     * Copies an asset from the application's assets to the cache directory.
     *
     * @param assetName the name of the asset to copy
     * @return the file representing the copied asset
     * @throws IOException if the asset could not be copied
     */
    private File copyAsset(String assetName) throws IOException {
        File target = cachedAsset(assetName);
        File partial = new File(target.getAbsolutePath() + ".part");
        File parent = partial.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create model directory: " + parent);
        }
        AssetManager assets = context.getAssets();
        try (InputStream input = assets.open(assetName);
             OutputStream output = new FileOutputStream(partial)) {
            byte[] buffer = new byte[1024 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        }
        if (!partial.renameTo(target)) {
            throw new IOException("Could not finalize embedded model asset: " + target);
        }
        return target;
    }

    /**
     * Returns the cached file for the given asset name.
     *
     * @param assetName the name of the asset
     * @return the file representing the cached asset
     */
    private File cachedAsset(String assetName) {
        return new File(context.getFilesDir(), assetName);
    }

    /**
     * Generates the expected SQL query for a given natural language question.
     *
     * @param question the natural language question
     * @return the expected SQL query
     */
    private static String expectedSql(String question) {
        Matcher transactionMatcher = TRANSACTION_ID.matcher(question);
        if (transactionMatcher.find()) {
            String transactionId = transactionMatcher.group().toUpperCase(Locale.ROOT);
            return "SELECT * FROM transactions WHERE transaction_id = '" + transactionId + "'";
        }
        String lower = question.toLowerCase(Locale.ROOT);
        if (lower.contains("all") || lower.contains("every") || lower.contains("list")) {
            return "SELECT * FROM transactions";
        }
        return "SELECT * FROM transactions WHERE description LIKE '%" + question.trim() + "%'";
    }

    /**
     * Summarizes the signals from the given rows of transaction data.
     *
     * @param rows the list of rows representing transaction data
     * @return a summary string of the signals
     */
    private static String summarySignals(List<Map<String, Object>> rows) {
        StringBuilder signals = new StringBuilder();
        for (Map<String, Object> row : rows) {
            if (signals.length() > 0) {
                signals.append("; ");
            }
            signals.append("transaction_id=").append(row.get("transaction_id"))
                    .append(", fraud_detected=").append(row.get("fraud_detected"))
                    .append(", status=").append(row.get("investigation_status"));
        }
        return signals.toString();
    }

    /**
     * Extracts the generated SQL query from the model's output.
     *
     * @param output the output from the model
     * @return the extracted SQL query
     * @throws IOException if no valid SQL query could be extracted
     */
    private static String extractGeneratedSql(String output) throws IOException {
        if (output == null || output.isBlank()) {
            throw new IOException("The embedded model returned no SQL");
        }
        int selectStart = output.toLowerCase(Locale.ROOT).indexOf("select");
        if (selectStart < 0) {
            throw new IOException("The embedded model returned no SELECT statement");
        }
        String sql = output.substring(selectStart).trim();
        int statementEnd = sql.indexOf(';');
        return (statementEnd >= 0 ? sql.substring(0, statementEnd) : sql).trim();
    }

    /**
     * Creates a native instance of the embedded Llama SQL model.
     *
     * @param modelPath the path to the model file
     * @param adapterPath the path to the adapter file
     * @return a handle to the native instance
     */
    private static native long nativeCreate(String modelPath, String adapterPath);

    /**
     * Generates SQL using the native embedded Llama SQL model.
     *
     * @param handle the handle to the native instance
     * @param prompt the natural language prompt
     * @param maxTokens the maximum number of tokens to generate
     * @return the generated SQL query
     */
    private static native String nativeGenerate(long handle, String prompt, int maxTokens);

    /**
     * Destroys the native instance of the embedded Llama SQL model.
     *
     * @param handle the handle to the native instance
     */
    private static native void nativeDestroy(long handle);
}
