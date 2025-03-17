package telran.monitoring;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import telran.monitoring.api.SensorData;

public class DynamoDbStreamSensorData extends DynamoDbStream<SensorData> {

    public DynamoDbStreamSensorData(String table) {
        super(table);
    }

    @Override
    Map<String, AttributeValue> getMap(SensorData sensorData) {
        HashMap<String, AttributeValue> map = new HashMap<>() {
            {
                put("patientId", AttributeValue.builder().n(sensorData.patientId() + "").build());
                put("value", AttributeValue.builder().n(sensorData.value() + "").build());
                put("timestamp", AttributeValue.builder().n(sensorData.timestamp() + "").build());
            }
        };
        return map;
    }

}
