package telran.monitoring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import telran.monitoring.api.Range;

public class RangeDataSource extends DataSourceBase {
    private static final String MAX_PULSE_VALUE = "max_pulse_value";
    private static final String MIN_PULSE_VALUE = "min_pulse_value";
    private static final String QUERY = "SELECT " + MIN_PULSE_VALUE + ", " + MAX_PULSE_VALUE
            + " FROM groups WHERE id = (SELECT group_id FROM patients WHERE patient_id = ?)";

    public RangeDataSource() {
        super(QUERY);
    }

    public Range getRange(long patientID) {
        try (Connection connection = DatabaseConfig.createConnection();
                PreparedStatement statement = createStatement(connection)) {

            statement.setLong(1, patientID);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return new Range(rs.getInt(MIN_PULSE_VALUE), rs.getInt(MAX_PULSE_VALUE));
            } else {
                throw new NoSuchElementException(String.format("Patient ID %d not found", patientID));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }
}