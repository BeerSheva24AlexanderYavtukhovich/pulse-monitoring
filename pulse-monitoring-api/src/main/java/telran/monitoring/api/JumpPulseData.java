package telran.monitoring.api;

import org.json.JSONObject;

public record JumpPulseData(long patientId, int oldValue, int newValue, long timestamp) {
    public static JumpPulseData of(String jsonStr) {
        JSONObject jsonObj = new JSONObject(jsonStr);
        long patientId = jsonObj.getLong("patientId");
        int oldValue = jsonObj.getInt("oldValue");
        int newValue = jsonObj.getInt("newValue");
        long timestamp = jsonObj.getLong("timestamp");
        return new JumpPulseData(patientId, oldValue, newValue, timestamp);

    }

    @Override
    public String toString() {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("patientId", patientId);
        jsonObj.put("oldValue", oldValue);
        jsonObj.put("newValue", newValue);
        jsonObj.put("timestamp", timestamp);
        return jsonObj.toString();
    }
}