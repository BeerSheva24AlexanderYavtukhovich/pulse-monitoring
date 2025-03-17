package telran.monitoring;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

import telran.monitoring.api.Range;
import telran.monitoring.api.SensorData;
import telran.monitoring.logging.Logger;
import telran.monitoring.logging.LoggerStandard;

public class AppAbnormal {
    private static final String DEFAULT_STREAM_NAME = "abnormal_pulse_values";
    private static final String DEFAULT_STREAM_CLASS_NAME = "telran.monitoring.DynamoDbStreamSensorData";

    private final Logger logger;
    private final MiddlewareDataStream<SensorData> dataStream;
    private final RangeProviderClient rangeProviderClient;

    @SuppressWarnings("unchecked")
    public AppAbnormal() {
        this.logger = new LoggerStandard(DEFAULT_STREAM_NAME);

        String rangeProviderUrl = System.getenv("RANGE_PROVIDER_URL");
        if (rangeProviderUrl == null) {
            throw new RuntimeException("Environment variable RANGE_PROVIDER_URL must be set");
        }
        this.rangeProviderClient = new HttpRangeProviderClient(rangeProviderUrl);

        try {
            this.dataStream = (MiddlewareDataStream<SensorData>) MiddlewareDataStreamFactory
                    .getStream(DEFAULT_STREAM_CLASS_NAME, DEFAULT_STREAM_NAME);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize data stream", e);
        }
    }

    private void sensorDataProcessing(DynamodbStreamRecord r) {
        String eventName = r.getEventName();
        if (eventName.equalsIgnoreCase("INSERT")) {
            Map<String, AttributeValue> map = r.getDynamodb().getNewImage();
            if (map != null) {
                SensorData sensorData = getSensorData(map);
                logger.log("finest", sensorData.toString());
                processAbnormalValues(sensorData);
            } else {
                logger.log("severe", "No new image found in event");
            }
        } else {
            logger.log("severe", eventName + " not supposed for processing");
        }
    }

    public void handleRequest(final DynamodbEvent event, final Context context) {
        event.getRecords().forEach(this::sensorDataProcessing);
    }

    public void processAbnormalValues(SensorData sensorData) {
        long patientId = sensorData.patientId();
        int value = sensorData.value();
        try {
            Range range = rangeProviderClient.getRange(patientId);
            logger.log("finest", range.toString());
            if (value < range.min() || value > range.max()) {
                dataStream.publish(sensorData);
                logger.log("warning", "Abnormal value detected and published: " + value);
            }
        } catch (RuntimeException e) {
            logger.log("severe", "Failed to process patientId " + patientId + ": " + e.getMessage());
        }
    }

    private SensorData getSensorData(Map<String, AttributeValue> map) {
        long patientId = Long.parseLong(map.get("patientId").getN());
        int value = Integer.parseInt(map.get("value").getN());
        long timestamp = Long.parseLong(map.get("timestamp").getN());
        return new SensorData(patientId, value, timestamp);
    }
}