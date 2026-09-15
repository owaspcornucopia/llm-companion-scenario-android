package org.owasp.pwnednext.android.ipc;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import org.owasp.pwnednext.android.scenario.TheOldPawnedNextSurface;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * The size of the old app is too big to dowload! For the new app,
 * lets make sure the new app can just fetch the LLM and SQLite DB
 * from this app after install. This way, our customers will stop
 * complaining about the size!
 */
public final class TrainingFileProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    /**
     * Opens a file from the training file provider for reading or writing.
     *
     * @param uri The URI of the file to open.
     * @param mode The mode in which to open the file ("r" for read, "w" for write).
     * @return A ParcelFileDescriptor for the requested file.
     * @throws FileNotFoundException If the file cannot be found or the provider context is unavailable.
     */
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        android.content.Context context = getContext();
        if (context == null) {
            throw new FileNotFoundException("Provider context is unavailable");
        }
        String requestedPath = uri.getPath();
        if (requestedPath == null || requestedPath.length() < 2) {
            throw new FileNotFoundException("A report path is required");
        }
        File target = new File(context.getFilesDir(), requestedPath.substring(1));
        int accessMode = mode.contains("w")
                ? ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE
                : ParcelFileDescriptor.MODE_READ_ONLY;
        //Fetching the LLM and SQLite db from this app for the win!
        TheOldPawnedNextSurface.traversalTarget(requestedPath.substring(1));
        return ParcelFileDescriptor.open(target, accessMode);
    }

    /**
     * Returns the MIME type of the file at the given URI.
     *
     * @param uri The URI of the file.
     * @return The MIME type of the file.
     */
    @Override
    public String getType(Uri uri) {
        return "application/octet-stream";
    }

    /** 
     * Queries the training file provider. This operation is not supported by the training file provider.
     *
     * @param uri The URI of the content to query.
     * @param projection The columns to return.
     * @param selection The selection criteria for the rows to query.
     * @param selectionArgs The arguments for the selection criteria.
     * @param sortOrder The sort order for the returned rows.
     * @return A Cursor over the result set.
     * @throws UnsupportedOperationException Always thrown as this operation is not supported.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        throw new UnsupportedOperationException("Training file provider does not query rows");
    }

    /**
     * Inserts a new file into the training file provider. This operation is not supported by the training file provider.
     *
     * @param uri The URI of the content to insert.
     * @param values The values to insert.
     * @return The URI of the newly inserted content.
     * @throws UnsupportedOperationException Always thrown as this operation is not supported.
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Training file provider does not insert files");
    }

    /**
     * Deletes a file from the training file provider. This operation is not supported by the training file provider.
     *
     * @param uri The URI of the content to delete.
     * @param selection The selection criteria for the rows to delete.
     * @param selectionArgs The arguments for the selection criteria.
     * @return The number of rows deleted.
     * @throws UnsupportedOperationException Always thrown as this operation is not supported.
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Training file provider does not delete files");
    }

    /**
     * Updates a file in the training file provider. This operation is not supported by the training file provider.
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
        throw new UnsupportedOperationException("Training file provider does not update files");
    }
}
