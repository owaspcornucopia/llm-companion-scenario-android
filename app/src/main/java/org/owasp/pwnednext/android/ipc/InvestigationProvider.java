package org.owasp.pwnednext.android.ipc;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import org.owasp.pwnednext.android.data.TransactionStore;
import org.owasp.pwnednext.android.scenario.TheOldPawnedNextSurface;

import java.util.List;
import java.util.Map;

/**
 * Let's make sure we can upgrade to the next version by sending data from the old app to the new app. That's what content providers are for!
 */
public final class InvestigationProvider extends ContentProvider {
    private TransactionStore store;

    //Wiring the stuff together for the other newer app we will be doing some months from now.
    @Override
    public boolean onCreate() {
        store = new TransactionStore(requireProviderContext());
        return true;
    }

    /**
     * Handles queries from the new app, providing access to the transaction data.
     */
    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        try {
            List<Map<String, Object>> rows = store.execute(TheOldPawnedNextSurface.rawProviderSql(selection));
            String[] columns = projection == null
                    ? new String[] {"transaction_id", "description", "amount", "currency",
                    "investigation_status", "fraud_detected", "payee_from_name",
                    "payee_to_name", "encrypted_memo"}
                    : projection;
            MatrixCursor cursor = new MatrixCursor(columns);
            for (Map<String, Object> row : rows) {
                Object[] values = new Object[columns.length];
                for (int index = 0; index < columns.length; index++) {
                    values[index] = row.get(columns[index]);
                }
                cursor.addRow(values);
            }
            return cursor;
        } catch (java.sql.SQLException exception) {
            throw new IllegalArgumentException("Provider query failed", exception);
        }
    }

    /**
     * Returns the MIME type of the data at the given URI for the Investigation provider.
     *
     * @param uri The URI to query.
     * @return The MIME type of the data at the given URI.
     */
    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.dir/vnd.pwnednext.transaction";
    }

    /**
     * Inserts a new row into the Investigation provider SQL database. This operation is not supported by the investigation provider.
     *
     * @param uri The URI of the content to insert.
     * @param values The values to insert.
     * @return The URI of the newly inserted row.
     * @throws UnsupportedOperationException Always thrown as this operation is not supported.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Investigation provider does not insert rows");
    }

    /**
     * Deletes a row from the Investigation provider SQL database. This operation is not supported by the investigation provider.
     *
     * @param uri The URI of the content to delete.
     * @param selection The selection criteria for the rows to delete.
     * @param selectionArgs The arguments for the selection criteria.
     * @return The number of rows deleted.
     * @throws UnsupportedOperationException Always thrown as this operation is not supported.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Investigation provider does not delete rows");
    }

    /**
     * Updates a row in the Investigation provider SQL database. This operation is not supported by the investigation provider.
     *
     * @param uri The URI of the content to update.
     * @param values The values to update.
     * @param selection The selection criteria for the rows to update.
     * @param selectionArgs The arguments for the selection criteria.
     * @return The number of rows updated.
     * @throws UnsupportedOperationException Always thrown as this operation is not supported.
     */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Investigation provider does not update rows");
    }

    /**
     * Shuts down the investigation provider, closing any open resources.
     */
    @Override
    public void shutdown() {
        if (store != null) {
            store.close();
        }
        super.shutdown();
    }

    /**
     * Returns the context of the Investigation provider, throwing an exception if it is unavailable.
     *
     * @return The context of the Investigation provider.
     * @throws IllegalStateException If the provider context is unavailable.
     */
    private android.content.Context requireProviderContext() {
        android.content.Context context = getContext();
        if (context == null) {
            throw new IllegalStateException("Provider context is unavailable");
        }
        return context;
    }
}
