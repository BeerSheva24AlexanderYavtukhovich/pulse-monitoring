package telran.monitoring;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.bson.Document;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import telran.monitoring.logging.Logger;
import telran.monitoring.logging.LoggerStandard;

public class AvgValuesPopulator {
    private static final String DEFAULT_STREAM_NAME = "average_pulse_values";
    private static final String MONGO_DB_NAME = "monitoring";
    private static final String MONGO_COLLECTION_NAME = "average_pulse_data";
    private Map<String, String> env = System.getenv();
    private String streamName = getStreamName();
    private Logger logger = new LoggerStandard(streamName);
    private MongoCollection<Document> collection;

    private final MongoClient mongoClient;

    public AvgValuesPopulator() {
        String connectionString = env.getOrDefault("MONGO_URI", "mongodb://localhost:27017");
        mongoClient = MongoClients.create(connectionString);

        try {
            MongoDatabase database = mongoClient.getDatabase(MONGO_DB_NAME);
            database.runCommand(new Document("ping", 1));
            logger.log("info", "Successfully connected to MongoDB");
            collection = database.getCollection(MONGO_COLLECTION_NAME);
        } catch (MongoException e) {
            logger.log("severe", "MongoDB connection error: " + e.getMessage());
            throw e;
        }
    }

    public void handleRequest(final DynamodbEvent event, final Context context) {
        if (event == null || event.getRecords() == null || event.getRecords().isEmpty()) {
            logger.log("warning", "Received an empty or null event with no records to process.");
            return;
        }
        event.getRecords().forEach(this::processRecord);
    }

    private String getStreamName() {
        return env.getOrDefault("STREAM_NAME", DEFAULT_STREAM_NAME);
    }

    private void processRecord(DynamodbStreamRecord record) {
        if ("INSERT".equalsIgnoreCase(record.getEventName())) {
            Map<String, AttributeValue> newImage = record.getDynamodb().getNewImage();
            if (newImage != null) {
                long patientId = Long.parseLong(newImage.get("patientId").getN());
                int avgValue = Integer.parseInt(newImage.get("value").getN());
                long timestamp = Long.parseLong(newImage.get("timestamp").getN());
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());

                Document document = new Document("patientId", patientId)
                        .append("avgValue", avgValue)
                        .append("dateTime", dateTime.toString());

                collection.insertOne(document);
                logger.log("info", "Inserted document for patient " + patientId + ": " + document.toJson());
            } else {
                logger.log("severe", "No new image found in event");
            }
        } else {
            logger.log("severe", "Unexpected event type: " + record.getEventName());
        }
    }
}