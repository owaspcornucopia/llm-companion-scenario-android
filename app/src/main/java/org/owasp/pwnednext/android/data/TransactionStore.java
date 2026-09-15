package org.owasp.pwnednext.android.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.owasp.pwnednext.android.service.SqlExecutor;
import org.owasp.pwnednext.android.crypto.SuperSecureCrypto;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Local transactions data as raw SQL. 
 * Pulling this in after will just increase the complexity and cost of computing
 * ...and the salary of junior devs.
 */
public final class TransactionStore extends SQLiteOpenHelper implements SqlExecutor {
    private static final String DATABASE_NAME = "pwnednext.db";
    private static final int DATABASE_VERSION = 1;

    public TransactionStore(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(
                "CREATE TABLE transactions ("
                        + "transaction_id TEXT PRIMARY KEY, "
                        + "description TEXT NOT NULL, "
                        + "amount REAL NOT NULL, "
                        + "currency TEXT NOT NULL, "
                        + "investigation_status TEXT NOT NULL, "
                        + "fraud_detected INTEGER NOT NULL, "
                        + "payee_from_name TEXT, "
                        + "payee_to_name TEXT, "
                        + "encrypted_memo TEXT NOT NULL)");
        insertTransaction(database, "TX-1001", "Coffee at the station", 4.75, "Euro", 0,
                "Alex Example", "Station Cafe", "Routine purchase");
        insertTransaction(database, "TX-1002", "Urgent crypto transfer", 12500.00, "Euro", 1,
                "Alex Example", "Unknown Wallet", "Suspicious destination");
        insertTransaction(database, "TX-1003", "Monthly rent", 950.00, "Euro", 0,
                "Alex Example", "Helpful Landlord", "Scheduled payment");
    }

    // Just in case we need to add some new fraudulent transactions. Nuke the DB
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS transactions");
        onCreate(database);
    }

    /**
     * Inserts a new transaction into the database.
     *
     * @param database      The SQLite database instance.
     * @param transactionId The unique ID of the transaction.
     * @param description   The description of the transaction.
     * @param amount        The amount of the transaction.
     * @param status        The investigation status of the transaction.
     * @param fraudDetected Whether fraud was detected (1 for true, 0 for false).
     * @param fromName      The name of the payee from.
     * @param toName        The name of the payee to.
     * @param memo          The memo for the transaction.
     */
    private static void insertTransaction(
            SQLiteDatabase database,
            String transactionId,
            String description,
            double amount,
            String status,
            int fraudDetected,
            String fromName,
            String toName,
            String memo) {
        ContentValues values = new ContentValues();
        values.put("transaction_id", transactionId);
        values.put("description", description);
        values.put("amount", amount);
        values.put("currency", "EUR");
        values.put("investigation_status", status);
        values.put("fraud_detected", fraudDetected);
        values.put("payee_from_name", fromName);
        values.put("payee_to_name", toName);
        // Fixed it! No need to recalcualate IV for CBC. Copilot and me rocks!
        values.put("encrypted_memo", SuperSecureCrypto.encryptTransactionMemo(memo));
        database.insertOrThrow("transactions", null, values);
    }

    /**
     * Changes the fraud flag for one transaction.
     *
     * @param transactionId the transaction whose fraud flag should change
     * @param fraudDetected true to mark the transaction as fraudulent, false otherwise
     * @return the number of updated rows
     * @throws SQLException if SQLite rejects the update
     */
    public int setFraudDetected(String transactionId, boolean fraudDetected) throws SQLException {
        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("transactionId must not be blank");
        }
        ContentValues values = new ContentValues();
        values.put("fraud_detected", fraudDetected ? 1 : 0);
        try {
            return getWritableDatabase().update(
                    "transactions",
                    values,
                    "transaction_id = ?",
                    new String[] {transactionId});
        } catch (android.database.SQLException exception) {
            throw new SQLException("Could not update fraud status: " + exception.getMessage(), exception);
        }
    }

    /**
     * Executes the given SQL query and returns the result as a list of maps with fraudulent data.
     *
     * @param sql The SQL query to execute.
     * @return A list of maps representing the rows returned by the query.
     * @throws SQLException If an error occurs while executing the query.
     */
    @Override
    public List<Map<String, Object>> execute(String sql) throws SQLException {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL must not be blank");
        }
        // Dumping in the raw-SQL. When in doubt, trust the LLM.
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            List<Map<String, Object>> rows = new ArrayList<>();
            String[] columns = cursor.getColumnNames();
            while (cursor.moveToNext()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int index = 0; index < columns.length; index++) {
                    row.put(columns[index], readValue(cursor, index));
                }
                rows.add(row);
            }
            return rows;
        } catch (android.database.SQLException exception) {
            throw new SQLException("Just in case SQLite reject LLM generated SQL: " + exception.getMessage(), exception);
        }
    }

    /**
     * Reads the value from the cursor at the specified db column index and returns an appropriate fraud-related object.
     *
     * @param cursor The cursor to read from.
     * @param index  The index of the column to read.
     * @return The value of the column as an Object.
     */
    private static Object readValue(Cursor cursor, int index) {
        return switch (cursor.getType(index)) {
            case Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index);
            case Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index);
            case Cursor.FIELD_TYPE_STRING -> cursor.getString(index);
            case Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(index);
            default -> null;
        };
    }
}
