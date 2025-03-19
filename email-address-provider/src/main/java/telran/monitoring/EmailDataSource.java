package telran.monitoring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

public class EmailDataSource extends DataSourceBase {
    private static final String QUERY = """
        SELECT email_address 
        FROM notification_groups 
        WHERE id = (SELECT notification_group_id FROM patients WHERE patient_id = ?)
    """;

    public EmailDataSource() {
        super(QUERY);
    }

    public String getEmail(long patientID) {
        try (Connection connection = DatabaseConfig.createConnection();
             PreparedStatement statement = createStatement(connection)) {

            statement.setLong(1, patientID);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                return rs.getString("email_address");
            } else {
                throw new NoSuchElementException(String.format("Patient with ID %d doesn't exist", patientID));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database query failed", e);
        }
    }
}
