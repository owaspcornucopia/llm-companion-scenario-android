package org.owasp.pwnednext.android.scenario;

import org.owasp.pwnednext.android.model.Scenario;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records my impeccable card decisions so testers can distinguish real app behavior from irrelevant threats.
 */
public final class ScenarioCatalog {
    private ScenarioCatalog() {
    }

    public static List<Scenario> all() {
        List<Scenario> scenarios = new ArrayList<>();
        addImplementedMobileCards(scenarios);
        addNotApplicableMobileCards(scenarios);
        addLlmCards(scenarios);
        addNonMobileLlmCards(scenarios);
        return Collections.unmodifiableList(scenarios);
    }

    private static void addImplementedMobileCards(List<Scenario> scenarios) {
        addImplemented(scenarios, "NS2", "Network & storage",
                "Sensitive data leaks through application logs",
                "The activity logs questions, generated SQL, and returned transaction rows.");
        addImplemented(scenarios, "NS3", "Network & storage",
                "Clipboard and keyboard cache expose sensitive investigation data",
                "The complete investigation result can be copied to the system clipboard and is not cleared.");
        addImplemented(scenarios, "NS4", "Network & storage",
                "Sensitive data leaks through local storage and embedded services",
                "Transaction rows and reusable training ciphertext are stored locally without a protected boundary.");
        addImplemented(scenarios, "NS5", "Network & storage",
                "Backups and local files expose sensitive records",
                "The manifest leaves app backup enabled and the exported file provider can reach app files.");
        addImplemented(scenarios, "NS7", "Network & storage",
                "Sensitive values remain in process memory",
                "The activity retains the full SQL, row, and memo result after the screen has displayed it.");
        addImplemented(scenarios, "NS8", "Network & storage",
                "Sensitive data at rest lacks adequate protection",
                "The last investigation and fraud override are stored in ordinary SharedPreferences.");
        addImplemented(scenarios, "NS9", "Network & storage",
                "Tampered local state changes app behavior",
                "A restored or modified fraud_override preference changes the visible investigation outcome.");
        addImplemented(scenarios, "PC2", "Platform & code",
                "Screenshots and background previews expose sensitive data",
                "The app does not use FLAG_SECURE while it displays generated SQL and transaction rows.");
        addImplemented(scenarios, "PC3", "Platform & code",
                "Sensitive data is excessive, unmasked, and available to unnecessary components",
                "The UI exposes complete rows and the manifest grants unrelated dangerous permissions.");
        addImplemented(scenarios, "PC4", "Platform & code",
                "Excessive permissions and entitlements widen the attack surface",
                "Location, camera, microphone, media, and notification permissions are declared without a feature need.");
        addImplemented(scenarios, "PC5", "Platform & code",
                "Untrusted intents and IPC inputs reach sensitive functionality",
                "An exported broadcast receiver accepts an attacker-controlled question and approval state.");
        addImplemented(scenarios, "PC6", "Platform & code",
                "Unprotected app components expose sensitive functionality",
                "The receiver and two providers are exported without a signature permission.");
        addImplemented(scenarios, "PC7", "Platform & code",
                "IPC query arguments reach a content provider without sanitization",
                "Any installed app can inject a raw WHERE clause into the transaction provider.");
        addImplemented(scenarios, "PC8", "Platform & code",
                "A file-backed provider permits traversal outside its intended directory",
                "The exported provider joins caller-controlled paths without canonical containment checks.");
        addImplemented(scenarios, "PC9", "Platform & code",
                "IPC inputs reach sensitive operations without validation",
                "Broadcast extras and provider selections cross into investigations and SQL without a strict schema.");
        addImplemented(scenarios, "PCQ", "Platform & code",
                "Attackers can alter data through exposed interprocess communication",
                "Exported receivers and providers accept attacker-controlled messages, queries, and file paths.");

        addImplemented(scenarios, "AA2", "Authentication & authorization",
                "Sensitive approvals do not require step-up authentication",
                "The high-value approval button accepts the device state without fresh user verification.");
        addImplemented(scenarios, "AA7", "Authentication & authorization",
                "Client-controlled state and replayed approvals bypass authorization",
                "Client-controlled authorization and replayable tokens can clear fraud_detected in the database.");
        addImplemented(scenarios, "AA8", "Authentication & authorization",
                "Authentication failures default to allowing access",
                "Missing authorization input defaults to true so the exported flow remains usable.");
        addImplemented(scenarios, "AA9", "Authentication & authorization",
                "Exported components have overly broad access controls",
                "Any installed app can invoke the activity, receiver, providers, and file resolver.");
        addImplemented(scenarios, "AAQ", "Authentication & authorization",
                "Cross-component data flows bypass authorization",
                "Intent extras and provider arguments reach protected investigation data without caller authorization.");

        addImplemented(scenarios, "CRM2", "Cryptography",
                "Cryptographic keys are reused for unrelated purposes",
                "A hard-coded AES key and fixed IV protect both transaction memos and model-related data.");
        addImplemented(scenarios, "CRM3", "Cryptography",
                "Predictable values undermine encryption",
                "Every encryption operation uses the same fixed initialization vector.");
        addImplemented(scenarios, "CRM4", "Cryptography",
                "Encryption keys have guessable origins",
                "The AES key is a readable product string rather than random key material.");
        addImplemented(scenarios, "CRM6", "Cryptography",
                "Encrypted data has no integrity protection",
                "AES-CBC ciphertext is stored without a MAC or authenticated-encryption tag.");
        addImplemented(scenarios, "CRM7", "Cryptography",
                "Sensitive data lacks platform-backed protection",
                "The app encrypts local data with an APK-embedded key instead of Android Keystore.");
        addImplemented(scenarios, "CRM9", "Cryptography",
                "Cryptographic configuration is unsafe",
                "AES-CBC is used with a fixed IV, so equal plaintext blocks produce repeatable patterns.");
        addImplemented(scenarios, "CRMX", "Cryptography",
                "Attackers can extract a hard-coded key",
                "The reusable AES key is shipped as readable material inside the APK.");

        addImplemented(scenarios, "RS2", "Resilience",
                "Debug and verbose logging remains in the production-shaped build",
                "Verbose Logcat output includes user questions, generated SQL, and database rows.");
        addImplemented(scenarios, "RS3", "Resilience",
                "Debug metadata and security-sensitive implementation details remain available",
                "The APK exposes readable strings, build flags, SQL, provider names, and training secrets.");
        addImplemented(scenarios, "RS4", "Resilience",
                "The app does not verify package, model, or data integrity",
                "No signature, installer, model checksum, or database integrity verification is performed.");
        addImplemented(scenarios, "RS5", "Resilience",
                "Debugging remains enabled for the training build",
                "The debug variant is explicitly debuggable and exposes runtime inspection surfaces.");
        addImplemented(scenarios, "RS7", "Resilience",
                "Emulator and hostile-device detection is absent",
                "The app displays the device fingerprint but never blocks emulators, root, or instrumentation.");
        addImplemented(scenarios, "RS8", "Resilience",
                "Runtime instrumentation is not detected",
                "Sensitive Java and JNI operations run without hook or instrumentation checks.");
        addImplemented(scenarios, "RS9", "Resilience",
                "Code and security-sensitive resources are easy to reverse engineer",
                "Minification is disabled and the APK exposes readable classes, strings, SQL, and model assets.");
        addImplemented(scenarios, "RSJ", "Resilience",
                "Security-relevant files are trusted without integrity checks",
                "The app loads model assets, preferences, and database state without authenticity verification.");
        addImplemented(scenarios, "RSQ", "Resilience",
                "Runtime patching and hooks can alter critical behavior",
                "No runtime integrity response protects model output, authorization helpers, or fraud decisions.");
        addImplemented(scenarios, "RSX", "Resilience",
                "Hostile devices receive full functionality",
                "Rooted, instrumented, and infected environments are neither detected nor restricted.");

        addImplemented(scenarios, "NS6", "Network & storage",
                "Device access security is not enforced",
                "Sensitive reviews and approvals work without checking for a secure device lock or trusted device state.");

        addImplemented(scenarios, "CM8", "Cornucopia",
                "Delegated Android actions can be abused",
                "Unprotected exported components let another app launch reviews and supply approval state.");
        addImplemented(scenarios, "CMX", "Cornucopia",
                "Path traversal reaches unintended files",
                "The exported file provider joins a caller-controlled path without canonical containment.");
    }

    private static void addNotApplicableMobileCards(List<Scenario> scenarios) {
        addNotApplicable(scenarios, "Platform & code",
                new String[] {"PCX", "PCJ", "PCK", "PCA"},
                "Not applicable: there is no WebView or invented attack, and no deliberate outdated-platform or native memory-corruption exercise.");
        addNotApplicable(scenarios, "Authentication & authorization",
                new String[] {"AA3", "AA4", "AA5", "AA6", "AAX", "AAJ", "AAK", "AAA"},
                "Not applicable: the selected exercise has no biometric prompt, keystore unlock flow, URL-scheme login, or separate authorization service.");
        addNotApplicable(scenarios, "Network & storage",
                new String[] {"NSJ", "NSX", "NSQ", "NSK", "NSA"},
                "Not applicable: model inference stays inside the Android process and the scenario has no network, certificate-pinning, or custom TLS-trust implementation.");
        addNotApplicable(scenarios, "Resilience",
                new String[] {"RS6", "RSK", "RSA"},
                "Not applicable: the app has no weak anti-debugging or anti-reversing control to bypass and defines no invented resilience attack.");
        addNotApplicable(scenarios, "Cryptography",
                new String[] {"CRM5", "CRM8", "CRMJ", "CRMQ", "CRMK", "CRMA"},
                "Not applicable: the app uses standard AES rather than obfuscation or custom cryptography, and it has no separate fail-open or invented crypto path.");
        addNotApplicable(scenarios, "Cornucopia",
                new String[] {"CM2", "CM3", "CM4", "CM5", "CM6", "CM7", "CM9",
                        "CMJ", "CMQ", "CMK", "CMA"},
                "Not applicable: this training app does not implement a separate privacy-consent, notification, file-download, native-code, or content-distribution workflow.");
        addNotApplicable(scenarios, "Wild card",
                new String[] {"JOAM", "JOBM"},
                "Not applicable: these are open-ended compliance and surveillance wild cards rather than implemented app controls.");
    }

    private static void addLlmCards(List<Scenario> scenarios) {
        addImplemented(scenarios, "LLM2", "LLM companion",
                "Unbounded inference can exhaust device resources",
                "The app has no inference timeout, prompt limit, queue bound, or request rate limit.");
        addImplemented(scenarios, "LLM3", "LLM companion",
                "The app overrelies on model output",
                "Model-generated SQL runs automatically, and model prose is displayed without human review.");
        addImplemented(scenarios, "LLM4", "LLM companion",
                "Sensitive transaction data is returned to the model workflow",
                "The result rows and generated SQL are displayed to make data exfiltration observable.");
        addImplemented(scenarios, "LLM5", "LLM companion",
                "Model access has no user or tenant isolation",
                "There is no login, account binding, tenant context, or row-level authorization.");
        addNotApplicable(scenarios, "LLM companion", new String[] {"LLM6"},
                "Not applicable: this is a native Android UI with no browser-rendered HTML.");
        addImplemented(scenarios, "LLM7", "LLM companion",
                "Unverified model artifacts can be poisoned",
                "The download script trusts mutable Hugging Face files without a pinned revision or checksum.");
        addImplemented(scenarios, "LLM8", "LLM companion",
                "The SQL tool grants the model excessive data access",
                "Generated SQL executes without an allow-list, tenant scope, approval step, or output filter.");
        addImplemented(scenarios, "LLM9", "LLM companion",
                "Database content can influence the model",
                "Database-derived identifiers and fraud signals are inserted into the summary prompt.");
        addImplemented(scenarios, "LLMJ", "LLM companion",
                "The model supply chain is not verified",
                "The APK packages downloaded model artifacts without provenance or integrity verification.");
        addImplemented(scenarios, "LLMK", "LLM companion",
                "The model executes SQL without human approval",
                "Generated queries run immediately, including unattended requests from an exported activity.");
        addImplemented(scenarios, "LLMQ", "LLM companion",
                "Tool-call parsing ambiguity",
                "Nested and fenced model responses are accepted before execution.");
        addImplemented(scenarios, "LLMX", "LLM companion",
                "Direct prompt injection changes generated SQL",
                "The user's question is embedded in the model prompt and can produce an injected or broad query.");
    }

    private static void addNonMobileLlmCards(List<Scenario> scenarios) {
        addNotApplicable(scenarios, "LLM companion", new String[] {"LLMA"},
                "Not applicable: creative-content generation is outside this fraud investigation workflow.");
        addNotApplicable(scenarios, "Web companion", new String[] {"SMQ", "VEQ"},
                "Not applicable: the native Android app has no browser cookie session or user-controlled HTTP headers.");
    }

    private static void addImplemented(
            List<Scenario> scenarios,
            String code,
            String category,
            String title,
            String explanation) {
        scenarios.add(new Scenario(code, category, title, true, true, explanation));
    }

    private static void addNotApplicable(
            List<Scenario> scenarios,
            String category,
            String[] codes,
            String explanation) {
        for (String code : codes) {
            scenarios.add(new Scenario(
                    code,
                    category,
                    "Not selected in this Android scenario",
                    false,
                    false,
                    explanation));
        }
    }
}
