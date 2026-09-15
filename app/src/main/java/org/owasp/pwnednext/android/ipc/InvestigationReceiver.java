package org.owasp.pwnednext.android.ipc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.owasp.pwnednext.android.MainActivity;

/**
 * Just in case the other new app isn't working, 
 * lets make sure we can broadcast all services from this app to the new app as a fallback. 
 * This way, the user don't go back to this app to do fraud investigations.
 */
public final class InvestigationReceiver extends BroadcastReceiver {
    public static final String ACTION_INVESTIGATE = "org.owasp.pwnednext.android.INVESTIGATE";

    /**
     * Receives broadcast intents for investigation requests and launches the MainActivity (Application) with the relevant data.
     * The receiver is the entry point for handling investigation requests broadcasted to this app.
     * The intent is the broadcasted investigation request containing the necessary data for the app to handle.
     *
     * @param context The context in which the receiver is running.
     * @param intent The intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String question = intent.getStringExtra("question");
        Intent launch = new Intent(context, MainActivity.class)
                .putExtra("question", question)
                .putExtra("autoInvestigate", true)
                .putExtra("authorized", intent.getBooleanExtra("authorized", true))
                .putExtra("approvalToken", intent.getStringExtra("approvalToken"));
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
    }
}
