package telran.monitoring;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL Driver not found", e);
        }
    }

    public static Connection createConnection() {
        try {
            String connectionStr = getEnvVar("DB_CONNECTION_STRING");
            String username = getEnvVar("DB_USERNAME");
            String password = getEnvVar("DB_PASSWORD");

            return DriverManager.getConnection(connectionStr, username, password);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to establish DB connection", e);
        }
    }

    private static String getEnvVar(String name) {
        String value = System.getenv(name);
        if (value == null) {
            throw new RuntimeException("Environment variable " + name + " must be set.");
        }
        return value;
    }
}