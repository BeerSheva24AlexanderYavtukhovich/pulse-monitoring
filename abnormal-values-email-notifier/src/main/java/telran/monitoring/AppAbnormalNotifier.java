package telran.monitoring;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent.DynamodbStreamRecord;
import com.amazonaws.services.lambda.runtime.events.models.dynamodb.AttributeValue;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import telran.monitoring.api.SensorData;
import telran.monitoring.logging.Logger;
import telran.monitoring.logging.LoggerStandard;

public class AppAbnormalNotifier {
    private static final String RECEIVED_VALUE = "Received value ";
    private static final String ABNORMAL_VALUE_DETECTED = "Abnormal value detected";
    private static final String DEFAULT_STREAM_NAME = "abnormal_pulse_values_notifier";
    private final Map<String, String> env = System.getenv();

    private final Logger logger;
    private final EmailProviderClient emailProviderClient;

    @SuppressWarnings("unchecked")
    public AppAbnormalNotifier() {
        this.logger = new LoggerStandard(DEFAULT_STREAM_NAME);

        String emailProviderUrl = System.getenv("EMAIL_PROVIDER_URL");
        if (emailProviderUrl == null) {
            throw new RuntimeException("Environment variable EMAIL_PROVIDER_URL must be set");
        }
        this.emailProviderClient = new HttpEmailProviderClient(emailProviderUrl);

    }
    
    public void handleRequest(final DynamodbEvent event, final Context context) {
        logger.log("info", "Email provider: " + System.getenv("EMAIL_PROVIDER_URL"));
        logger.log("info", "Records received: " + event.getRecords().size());
        event.getRecords().forEach(this::sensorDataProcessing);
    }

    private void sensorDataProcessing(DynamodbStreamRecord r) {
        String eventName = r.getEventName();
        if (eventName.equalsIgnoreCase("INSERT")) {
            Map<String, AttributeValue> map = r.getDynamodb().getNewImage();
            if (map != null) {
                logger.log("finest", "Start getting sensor data");
                SensorData sensorData = getSensorData(map);
                logger.log("finest", sensorData.toString());
                sendEmail(sensorData);
            } else {
                logger.log("severe", "No new image found in event");
            }
        } else {
            logger.log("severe", eventName + " not supposed for processing");
        }
    }
    public void sendEmail(SensorData sensorData) {
        String SENDER_EMAIL = env.get("SENDER_EMAIL");
        String TESTING_PREFIX = env.getOrDefault("TESTING_PREFIX", "");
        long patientId = sensorData.patientId();
        int value = sensorData.value();
        try {
            String email = emailProviderClient.getEmail(patientId);
            logger.log("finest", "Email of patient: " + email);
            if (email != null) {
                logger.log("finest", "Email of sender: " + SENDER_EMAIL);
                logger.log("finest", "Email of receipent: " + TESTING_PREFIX + email);
                sendEmailViaSes(SENDER_EMAIL, TESTING_PREFIX + email, ABNORMAL_VALUE_DETECTED,
                        RECEIVED_VALUE + String.valueOf(value), logger);
            }
        } catch (RuntimeException e) {
            logger.log("severe", "Failed to process patientId " + patientId + ": " + e.getMessage());
        }
    }

    private static void sendEmailViaSes(String senderEmail, String recipientEmail, String subject, String bodyText,
            Logger logger) {
        SesClient sesClient = SesClient.create();

        Destination destination = Destination.builder()
                .toAddresses(recipientEmail)
                .build();

        Content subjectContent = Content.builder().data(subject).build();
        Content bodyContent = Content.builder().data(bodyText).build();
        Body body = Body.builder().text(bodyContent).build();
        Message message = Message.builder().subject(subjectContent).body(body).build();

        SendEmailRequest request = SendEmailRequest.builder()
                .source(senderEmail)
                .destination(destination)
                .message(message)
                .build();

        try {
            logger.log("info", "!!! sending disabled  !!!! ready to send message to " + recipientEmail);
           // SendEmailResponse response = sesClient.sendEmail(request);
           // logger.log("info", "Email sent, message ID: " + response.messageId());
        } catch (AwsServiceException | SdkClientException ex) {
            logger.log("severe", "Error sending email: " + ex.getMessage());
        }
    }

    private SensorData getSensorData(Map<String, AttributeValue> map) {
        long patientId = Long.parseLong(map.get("patientId").getN());
        int value = Integer.parseInt(map.get("value").getN());
        long timestamp = Long.parseLong(map.get("timestamp").getN());
        return new SensorData(patientId, value, timestamp);
    }
}