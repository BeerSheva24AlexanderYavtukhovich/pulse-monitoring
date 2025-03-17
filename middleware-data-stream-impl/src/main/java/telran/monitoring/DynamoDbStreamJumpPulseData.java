package telran.monitoring;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import telran.monitoring.api.JumpPulseData;

public class DynamoDbStreamJumpPulseData extends DynamoDbStream<JumpPulseData> {

    public DynamoDbStreamJumpPulseData(String table) {
        super(table);
    }

    @Override
    Map<String, AttributeValue> getMap(JumpPulseData jumpData) {
        HashMap<String, AttributeValue> map = new HashMap<>() {
            {
                put("patientId", AttributeValue.builder().n(jumpData.patientId() + "").build());
                put("oldValue", AttributeValue.builder().n(jumpData.oldValue() + "").build());
                put("newValue", AttributeValue.builder().n(jumpData.newValue() + "").build());
                put("timestamp", AttributeValue.builder().n(jumpData.timestamp() + "").build());
            }
        };
        return map;
    }

}