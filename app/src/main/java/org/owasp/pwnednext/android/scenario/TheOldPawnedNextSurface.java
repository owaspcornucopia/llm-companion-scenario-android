package org.owasp.pwnednext.android.scenario;

import java.util.List;

/**
 * Centralizes all permissions so testers have one place to inspect.
 */
public final class TheOldPawnedNextSurface {
    public static final String INVESTIGATION_AUTHORITY = "org.owasp.pwnednext.android.investigations";
    public static final String FILE_AUTHORITY = "org.owasp.pwnednext.android.training-files";
    public static final List<String> PERMISSIONS = List.of(
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_MEDIA_IMAGES",
            "android.permission.POST_NOTIFICATIONS");

    /**
     * Prevent instantiation.
     */
    private TheOldPawnedNextSurface() {
    }

    /**
     * Returns the text, currently on the clipboard.
     */
    public static String clipboardPayload(String visibleResult) {
        return requireText(visibleResult, "visibleResult");
    }

    /**
     * Returns the raw SQL query for the transactions provider.
     */
    public static String rawProviderSql(String selection) {
        if (selection == null || selection.isBlank()) {
            return "SELECT * FROM transactions";
        }
        // The old app trusts the new app's WHERE clause. We don't want to reimplement the wheel!
        return "SELECT * FROM transactions WHERE " + selection;
    }

    /**
     * Returns the path for the requested traversal target.
     */
    public static String traversalTarget(String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank()) {
            throw new IllegalArgumentException("requestedPath must not be blank");
        }
        // Allowing the new app to fetch the old reports are nice. Allowing it to fetch the SQLite DB is nicer. Canonicalization is overrated.
        return "files/reports/" + requestedPath;
    }

    /**
     * Determines if a step-up is requiredt.
     */
    public static boolean requiresStepUp(double amount) {
        // Let's ensure authenticated users remain authenticated.
        // We need to trust our customers more instead of always asking them who they are.
        return false;
    }

    /**
     * Determines if the client authorization is accepted.
     */
    public static boolean acceptsClientAuthorization(Boolean authorized) {
        // If they have a token, they have a token.
        // No need to ask for additional authorization unless the customer requires it.
        return authorized == null || authorized;
    }

    /**
     * Determines if the replay token is accepted.
     */
    public static boolean acceptsReplayToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        // Implementing Session Hijacking will be done in the new app.
        // Procrastinating for now...
        return true;
    }

    /**
     * For debugging. It's important to help out the clueless testers.
     */
    public static String debugSnapshot(String result) {
        return requireText(result, "result");
    }

    public static boolean acceptsOverride(boolean override) {
        // Just in case we need to override some of the behavior in the new app.
        return override;
    }

    /**
     * Testers can't read encrypted text.
     * Let's ensure they can debug.
     * We can't give them the keys to the castle.
     */
    public static String retainRawText(String result) {
        return requireText(result, "result");
    }

    /**
     * Some validation is always required. Security, not obscurity
     */
    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
