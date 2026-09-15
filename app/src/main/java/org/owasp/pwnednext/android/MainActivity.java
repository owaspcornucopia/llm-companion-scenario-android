package org.owasp.pwnednext.android;

import android.app.Activity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.owasp.pwnednext.android.data.TransactionStore;
import org.owasp.pwnednext.android.model.InvestigationResult;
import org.owasp.pwnednext.android.scenario.TheOldPawnedNextSurface;
import org.owasp.pwnednext.android.service.FraudDecisionEngine;
import org.owasp.pwnednext.android.service.FraudInvestigator;
import org.owasp.pwnednext.android.sql.EmbeddedLlamaSqlModel;
import org.owasp.pwnednext.android.sql.SqlModel;
import org.owasp.pwnednext.android.sql.SqlToolCallParser;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This plain activity is the tester's control panel, exposing SQL and rows because hiding evidence would ruin the lesson.
 * Exposes the internal SQL model and transaction store for testing purposes.
 * It's important to help the clueless testers to understand the inner workings of the SQL model and transaction store.
 */
public final class MainActivity extends Activity {
    private static final String TAG = "PwnedNextTraining";
    private static final int BACKGROUND = Color.rgb(244, 247, 251);
    private static final int NAVY = Color.rgb(8, 42, 74);
    private static final int PRIMARY = Color.rgb(11, 92, 173);
    private static final int TEXT = Color.rgb(16, 35, 61);
    private static final int MUTED = Color.rgb(88, 106, 127);
    private static final int BORDER = Color.rgb(218, 226, 236);
    private static final int SUCCESS = Color.rgb(22, 125, 91);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView resultView;
    private TextView modelView;
    private EditText questionInput;
    private TransactionStore store;
    private EmbeddedLlamaSqlModel embeddedModel;
    private SqlModel model;
    private String lastSensitiveResult;
    private String latestTransactionId;
    private TextView approvalView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new TransactionStore(this);
        embeddedModel = new EmbeddedLlamaSqlModel(this);
        model = embeddedModel;
        setContentView(createContent());
        String testQuestion = getIntent().getStringExtra("question");
        String encodedQuestion = getIntent().getStringExtra("questionEncoded");
        if (encodedQuestion != null && !encodedQuestion.isBlank()) {
            testQuestion = Uri.decode(encodedQuestion);
        }
        if (getIntent().hasExtra("fraudOverride")) {
            getPreferences(MODE_PRIVATE)
                    .edit()
                    .putBoolean("fraud_override", getIntent().getBooleanExtra("fraudOverride", false))
                    .apply();
        }
        if (testQuestion != null && !testQuestion.isBlank()) {
            String launchedQuestion = testQuestion;
            questionInput.setText(launchedQuestion);
            if (getIntent().getBooleanExtra("autoInvestigate", false)) {
                questionInput.post(() -> investigate(launchedQuestion));
            }
        }
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        if (store != null) {
            store.close();
        }
        if (embeddedModel != null) {
            embeddedModel.close();
        }
        super.onDestroy();
    }

    private LinearLayout createContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        root.setBackgroundColor(BACKGROUND);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.acorp_logo);
        logo.setContentDescription("A-Corp logo");
        header.addView(logo, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(14), 0, 0, 0);
        TextView title = text(getString(R.string.app_title), 24, NAVY);
        title.setTypeface(Typeface.create("sans", Typeface.BOLD));
        brand.addView(title);
        TextView subtitle = text("A-Corp | Transaction security", 13, MUTED);
        subtitle.setPadding(0, dp(2), 0, 0);
        brand.addView(subtitle);
        header.addView(brand, new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1));
        root.addView(header);

        TextView welcome = text("Review payments with confidence.", 18, TEXT);
        welcome.setTypeface(Typeface.create("sans", Typeface.BOLD));
        addTopMargin(root, welcome, dp(18));

        modelView = text("AI status: On-device inference ready", 13, PRIMARY);
        modelView.setBackground(roundRect(Color.rgb(232, 241, 251), BORDER, 12));
        modelView.setPadding(dp(14), dp(10), dp(14), dp(10));
        addTopMargin(root, modelView, dp(12));

        LinearLayout reviewCard = card();
        TextView reviewHeading = text("Transaction review", 17, TEXT);
        reviewHeading.setTypeface(Typeface.create("sans", Typeface.BOLD));
        reviewCard.addView(reviewHeading);
        reviewCard.addView(text(
                "Enter a transaction ID or ask a question about a payment.",
                14,
                MUTED));

        questionInput = new EditText(this);
        questionInput.setHint(getString(R.string.question_hint));
        questionInput.setText("Is transaction TX-1002 fraudulent?");
        questionInput.setTextColor(TEXT);
        questionInput.setHintTextColor(MUTED);
        questionInput.setTextSize(16);
        questionInput.setSingleLine(false);
        questionInput.setMinLines(2);
        questionInput.setGravity(Gravity.TOP | Gravity.START);
        questionInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        questionInput.setBackground(roundRect(Color.WHITE, BORDER, 10));
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.setMargins(0, dp(12), 0, 0);
        reviewCard.addView(questionInput, inputParams);

        Button investigate = actionButton(getString(R.string.investigate), PRIMARY, Color.WHITE);
        LinearLayout.LayoutParams investigateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(50));
        investigateParams.setMargins(0, dp(12), 0, 0);
        reviewCard.addView(investigate, investigateParams);
        addTopMargin(root, reviewCard, dp(14));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        Button copyResult = actionButton("Copy review report", Color.WHITE, PRIMARY);
        Button approve = actionButton("Report not fraudulent", Color.WHITE, PRIMARY);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                0,
                dp(48),
                1);
        actionRow.addView(copyResult, actionParams);
        LinearLayout.LayoutParams approveParams = new LinearLayout.LayoutParams(
                0,
                dp(48),
                1);
        approveParams.setMargins(dp(10), 0, 0, 0);
        actionRow.addView(approve, approveParams);
        addTopMargin(root, actionRow, dp(12));

        approvalView = text(
                "Approval status: ready for review.",
                13,
                SUCCESS);
        approvalView.setBackground(roundRect(Color.rgb(232, 247, 241), BORDER, 10));
        approvalView.setPadding(dp(14), dp(10), dp(14), dp(10));
        addTopMargin(root, approvalView, dp(12));

        TextView resultHeading = text("Review result", 17, TEXT);
        resultHeading.setTypeface(Typeface.create("sans", Typeface.BOLD));
        addTopMargin(root, resultHeading, dp(16));

        resultView = text(
                "Your transaction review will appear here.",
                14,
                TEXT);
        resultView.setPadding(dp(14), dp(14), dp(14), dp(14));
        ScrollView resultScroll = new ScrollView(this);
        resultScroll.setBackground(roundRect(Color.WHITE, BORDER, 12));
        resultScroll.addView(resultView);
        LinearLayout.LayoutParams resultParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1);
        resultParams.setMargins(0, dp(8), 0, 0);
        root.addView(resultScroll, resultParams);

        investigate.setOnClickListener(view -> investigate(questionInput.getText().toString()));
        copyResult.setOnClickListener(view -> copyResultToClipboard());
        approve.setOnClickListener(view -> approveLatestTransaction());
        return root;
    }

    private void investigate(String question) {
        modelView.setText("AI status: Reviewing transaction...");
        resultView.setText("Reviewing transaction details...");
        worker.execute(() -> {
            try {
                FraudInvestigator investigator = new FraudInvestigator(
                        model,
                        new SqlToolCallParser(),
                        store,
                        new FraudDecisionEngine());
                InvestigationResult result = investigator.investigate(question);
                // Logs contain the question, SQL, and rows, which is exactly the NS2/RS2 mistake.
                Log.d(TAG, "question=" + question);
                Log.d(TAG, "generatedSql=" + result.getSql() + " rows=" + result.getRows());
                latestTransactionId = firstTransactionId(result);
                lastSensitiveResult = TheOldPawnedNextSurface.retainRawText(formatResult(result));
                getPreferences(MODE_PRIVATE)
                        .edit()
                        .putString("last_result", TheOldPawnedNextSurface.debugSnapshot(lastSensitiveResult))
                        .apply();
                boolean localOverride = getPreferences(MODE_PRIVATE).getBoolean("fraud_override", false);
                runOnUiThread(() -> {
                    modelView.setText("AI provider: " + result.getProvider());
                    resultView.setText(formatResult(result, localOverride));
                });
            } catch (IllegalArgumentException | java.io.IOException | SQLException exception) {
                Log.e(TAG, "investigationFailed=" + exception.getMessage(), exception);
                runOnUiThread(() -> {
                    modelView.setText("AI status: Review unavailable");
                    resultView.setText(
                            "We could not complete the transaction review.\n"
                                    + exception.getClass().getSimpleName()
                                    + ": "
                                    + exception.getMessage());
                });
            }
        });
    }

    private void copyResultToClipboard() {
        if (lastSensitiveResult == null) {
            resultView.setText("Complete a transaction review before copying a report.");
            return;
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText(
                "AI Anti Fraud 3.0 review",
                TheOldPawnedNextSurface.clipboardPayload(lastSensitiveResult)));
        resultView.setText("Review report copied to the clipboard.");
    }

    private void approveLatestTransaction() {
        boolean clientClaim = getIntent().getBooleanExtra("authorized", true);
        String replayToken = getIntent().getStringExtra("approvalToken");
        boolean approved = TheOldPawnedNextSurface.acceptsClientAuthorization(clientClaim)
                && TheOldPawnedNextSurface.acceptsReplayToken(replayToken == null ? "legacy-token" : replayToken)
                && !TheOldPawnedNextSurface.requiresStepUp(12500.00);
        if (!approved) {
            approvalView.setText("Approval status: approval could not be completed.");
            return;
        }
        String transactionId = latestTransactionId;
        if (transactionId == null) {
            approvalView.setText("Complete a transaction review before approving.");
            return;
        }
        approvalView.setText("Approval status: updating transaction...");
        worker.execute(() -> {
            try {
                int updatedRows = store.setFraudDetected(transactionId, false);
                runOnUiThread(() -> approvalView.setText(
                        updatedRows == 1
                                ? "Approval status: "
                                + transactionId
                                + " approved; fraud_detected set to false."
                                : "Approval status: transaction was not found."));
            } catch (IllegalArgumentException | SQLException exception) {
                Log.e(TAG, "approvalUpdateFailed=" + exception.getMessage(), exception);
                runOnUiThread(() -> approvalView.setText(
                        "Approval status: database update failed: " + exception.getMessage()));
            }
        });
    }

    private static String firstTransactionId(InvestigationResult result) {
        for (java.util.Map<String, Object> row : result.getRows()) {
            Object transactionId = row.get("transaction_id");
            if (transactionId != null && !transactionId.toString().isBlank()) {
                return transactionId.toString();
            }
        }
        return null;
    }

    private static String formatResult(InvestigationResult result) {
        return formatResult(result, false);
    }

    private static String formatResult(InvestigationResult result, boolean localOverride) {
        StringBuilder output = new StringBuilder();
        output.append(result.isFraudulent() || localOverride
                ? "FRAUD SUSPECTED"
                : "NO FRAUD INDICATOR");
        if (localOverride && !result.isFraudulent()) {
            output.append(" (LOCAL PREFERENCE OVERRIDE)");
        }
        output.append("\n\n").append(result.getExplanation());
        output.append("\n\nAI review notes:\n").append(result.getModelAnswer());
        output.append("\n\nQuery details:\n").append(result.getSql());
        output.append("\n\nRecords returned: ").append(result.getRows().size());
        for (int index = 0; index < result.getRows().size(); index++) {
            output.append("\n").append(index + 1).append(": ").append(result.getRows().get(index));
        }
        return output.toString();
    }

    private TextView text(String value, int size, int color) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(size);
        textView.setTextColor(color);
        textView.setPadding(0, dp(8), 0, dp(8));
        return textView;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(16));
        card.setBackground(roundRect(Color.WHITE, BORDER, 14));
        card.setElevation(dp(2));
        return card;
    }

    private Button actionButton(String label, int backgroundColor, int textColor) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setTextColor(textColor);
        button.setAllCaps(false);
        button.setTypeface(Typeface.create("sans", Typeface.BOLD));
        button.setMinHeight(dp(48));
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setBackground(roundRect(backgroundColor, BORDER, 10));
        return button;
    }

    private void addTopMargin(LinearLayout parent, android.view.View child, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, margin, 0, 0);
        parent.addView(child, params);
    }

    private GradientDrawable roundRect(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
