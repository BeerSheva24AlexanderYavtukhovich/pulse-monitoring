package telran.monitoring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class DataSourceBase {
    private final String query;

    protected DataSourceBase(String query) {
        this.query = query;
    }

    protected PreparedStatement createStatement(Connection connection) {
        try {
            return connection.prepareStatement(query);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to prepare SQL statement", e);
        }
    }
}