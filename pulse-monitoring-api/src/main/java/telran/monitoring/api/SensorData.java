package telran.monitoring.api;

import org.json.JSONObject;

public record SensorData(long patientId, int value, long timestamp) {
    public static SensorData of(String jsonStr) {
        JSONObject jsonObj = new JSONObject(jsonStr);
        long patientId = jsonObj.getLong("patientId");
        int value = jsonObj.getInt("value");
        long timestamp = jsonObj.getLong("timestamp");
        return new SensorData(patientId, value, timestamp);

    }

    @Override
    public String toString() {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("patientId", patientId);
        jsonObj.put("value", value);
        jsonObj.put("timestamp", timestamp);
        return jsonObj.toString();
    }
}