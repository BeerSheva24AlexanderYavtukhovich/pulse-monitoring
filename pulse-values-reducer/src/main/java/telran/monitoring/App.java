package telran.monitoring;

import java.util.List;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

import telran.monitoring.api.LatestValuesSaver;
import telran.monitoring.api.SensorData;
import telran.monitoring.logging.Logger;
import telran.monitoring.logging.LoggerStandard;

public class App {
    private static final String DEFAULT_STREAM_CLASS_NAME = "telran.monitoring.DynamoDbStreamSensorData";
    private static final String DEFAULT_STREAM_NAME = "average_pulse_values";
    private static final int AVERAGE_RANGE = 5;
    private static final long TIME_THRESHOLD = 60_000;
    private Map<String, String> env = System.getenv();
    private String streamName = getStreamName();
    Logger logger = new LoggerStandard(streamName);
    MiddlewareDataStream<SensorData> dataStream;
    LatestValuesSaver sensorDataValuesList = new LatestValuesSaverMap(logger);

    @SuppressWarnings("unchecked")
    public App() {
        try {
            dataStream = (MiddlewareDataStream<SensorData>) MiddlewareDataStreamFactory.getStream(
                    getStreamClassName(), getStreamName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleRequest(final DynamodbEvent event, final Context context) {
        event.getRecords().forEach(this::sensorDataProcessing);
    }

    private String getStreamName() {
        return env.getOrDefault("STREAM_NAME", DEFAULT_STREAM_NAME);
    }

    private String getStreamClassName() {
        return env.getOrDefault("STREAM_CLASS_NAME", DEFAULT_STREAM_CLASS_NAME);
    }

    private void sensorDataProcessing(DynamodbStreamRecord r) {
        String eventName = r.getEventName();
        if (eventName.equalsIgnoreCase("INSERT")) {
            Map<String, AttributeValue> map = r.getDynamodb().getNewImage();
            if (map != null) {
                SensorData sensorData = getSensorData(map);
                logger.log("finest", sensorData.toString());
                processPatientData(sensorData);
            } else {
                logger.log("severe", "No new image found in event");
            }
        } else {
            logger.log("severe", eventName + " not supposed for processing");
        }
    }

    private void processPatientData(SensorData sensorData) {
        long patientId = sensorData.patientId();
        long lastValueTimestamp = sensorData.timestamp();
        long currentTime = System.currentTimeMillis();
        long currentThreshold = currentTime - lastValueTimestamp;

        sensorDataValuesList.addValue(sensorData);
        List<SensorData> patientData = sensorDataValuesList.getAllValues(patientId);

        if (patientData.size() == AVERAGE_RANGE
                || (!patientData.isEmpty() && (currentThreshold > TIME_THRESHOLD))) {
            int sum = patientData.stream().mapToInt(SensorData::value).sum();
            int averageValue = sum / patientData.size();
            SensorData averageSensorData = new SensorData(patientId, averageValue, currentTime);
            dataStream.publish(averageSensorData);
            logger.log("debug", "Processed batch average pulse for patient " + patientId + ": " + averageSensorData);
            sensorDataValuesList.clearValues(patientId);
        }
    }

    private SensorData getSensorData(Map<String, AttributeValue> map) {
        long patientId = Long.parseLong(map.get("patientId").getN());
        int value = Integer.parseInt(map.get("value").getN());
        long timestamp = Long.parseLong(map.get("timestamp").getN());
        return new SensorData(patientId, value, timestamp);
    }
}
