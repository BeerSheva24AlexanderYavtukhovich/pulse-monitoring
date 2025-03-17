package telran.monitoring;

import java.util.Map;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest.Builder;

public abstract class DynamoDbStream<T> implements MiddlewareDataStream<T> {
    DynamoDbClient client;
    Builder request;

    public DynamoDbStream(String table) {
        client = DynamoDbClient.builder().build();
        request = PutItemRequest.builder().tableName(table);
    }

    @Override
    public void publish(T obj) {
        Map<String, AttributeValue> mapItem = getMap(obj);
        client.putItem(request.item(mapItem).build());
    }

    abstract Map<String, AttributeValue> getMap(T obj);
}
