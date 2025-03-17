package telran.monitoring;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import telran.monitoring.api.Range;

public class DataSource {
    private static final String DEFAULT_DRIVER_CLASS_NAME = "org.postgresql.Driver";
    private static final String MIN_PULSE_VALUE = "min_pulse_value";
    private static final String MAX_PULSE_VALUE = "max_pulse_value";

    private PreparedStatement statement;
    private Connection con;

    static {
        try {
            Class.forName(DEFAULT_DRIVER_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public DataSource(String connectionStr, String username, String password) {
        try {
            con = DriverManager.getConnection(connectionStr, username, password);
            statement = con.prepareStatement(
                    String.format(
                            "SELECT %s, %s FROM groups WHERE id = (SELECT group_id FROM patients WHERE patient_id = ?)",
                            MIN_PULSE_VALUE, MAX_PULSE_VALUE
                    ));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Range getRange(long patientID) {
        try {
            statement.setLong(1, patientID);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int min = rs.getInt(MIN_PULSE_VALUE);
                int max = rs.getInt(MAX_PULSE_VALUE);
                return new Range(min, max);
            } else {
                throw new NoSuchElementException(String.format("Patient with ID %d doesn't exist", patientID));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}