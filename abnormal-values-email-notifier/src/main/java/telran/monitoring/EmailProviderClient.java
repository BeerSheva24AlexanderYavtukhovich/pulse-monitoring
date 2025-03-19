package telran.monitoring;

public interface EmailProviderClient {
    String getEmail(long patientId);
}
