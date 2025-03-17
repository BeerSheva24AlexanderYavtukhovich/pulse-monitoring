package telran.monitoring.api;

public record NotificationData(long patientId, String email,
        String notificationText, long timestamp) {
}
