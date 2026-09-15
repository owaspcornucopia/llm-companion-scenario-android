package org.owasp.pwnednext.android.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Executes SQL queries and returns the results as a list of maps, where each map represents a row in the result set.
 * This allows for decoupling the SQL execution logic from the rest of the application, making it easier to test and maintain.
 * 
 * Implementations of this interface are responsible for connecting to the database, executing the query, and mapping the result set to a list of maps.
 * 
 * @param sql the SQL query to execute
 * @return a list of maps representing the rows in the result set
 * @throws SQLException if a database access error occurs
 */
public interface SqlExecutor {
    List<Map<String, Object>> execute(String sql) throws SQLException;
}
