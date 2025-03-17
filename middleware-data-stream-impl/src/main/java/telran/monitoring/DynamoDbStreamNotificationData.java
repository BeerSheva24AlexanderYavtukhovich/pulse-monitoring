package telran.monitoring;

import java.util.HashMap;
import java.util.Map;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import telran.monitoring.api.NotificationData;

public class DynamoDbStreamNotificationData extends DynamoDbStream<NotificationData> {

    public DynamoDbStreamNotificationData(String table) {
        super(table);
    }

    @Override
    Map<String, AttributeValue> getMap(NotificationData notificationData) {
        HashMap<String, AttributeValue> map = new HashMap<>() {
            {
                put("patientId", AttributeValue.builder().n(notificationData.patientId() + "").build());
                put("email", AttributeValue.builder().n(notificationData.email() + "").build());
                put("notificationText", AttributeValue.builder().n(notificationData.notificationText() + "").build());
                put("timestamp", AttributeValue.builder().n(notificationData.timestamp() + "").build());
            }
        };
        return map;
    }

}
