package telran.monitoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import telran.monitoring.api.SensorData;
import telran.monitoring.logging.Logger;

public class LatestDataSaverS3 extends DataSaverLogger {
    private final String bucketName;
    private final S3Client s3Client;
    private final Map<String, String> env = System.getenv();

    public LatestDataSaverS3(Logger logger) {
        super(logger);
        logger.log("info", "Initializing LatestDataSaverS3...");
        logger.log("info", "Bucket name " + env.get("BUCKET_NAME"));
        this.bucketName = env.get("BUCKET_NAME");
        this.s3Client = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        logger.log("info", "LatestDataSaverS3 initialized with bucket: " + bucketName);
    }

    private String getS3Key(long patientId) {
        return "patients/" + patientId + ".json";
    }

    @Override
    public void addValue(SensorData sensorData) {
        long patientId = sensorData.patientId();
        logger.log("info", "Adding value for patientId: " + patientId + " -> " + sensorData);
        List<SensorData> dataList = getAllValues(patientId);
        dataList.add(sensorData);
        saveToS3(patientId, dataList);
    }

    @Override
    public List<SensorData> getAllValues(long patientId) {
        logger.log("info", "Fetching all values for patientId: " + patientId);
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(getS3Key(patientId))
                    .build();
            String json = s3Client.getObjectAsBytes(request).asUtf8String();

            if (json.isEmpty()) {
                logger.log("info", "No data found for patientId: " + patientId);
                return new ArrayList<>();
            }

            logger.log("info", "Data retrieved from S3 for patientId: " + patientId);
            return parseJsonToSensorDataList(json);
        } catch (NoSuchKeyException e) {
            logger.log("info", "No existing data found for patientId: " + patientId);
            return new ArrayList<>();
        } catch (AwsServiceException | SdkClientException e) {
            logger.log("error", "Error retrieving data from S3 for patientId: " + patientId + " -> " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public SensorData getLastValue(long patientId) {
        logger.log("info", "Fetching last value for patientId: " + patientId);
        List<SensorData> dataList = getAllValues(patientId);
        if (dataList.isEmpty()) {
            logger.log("info", "No data available for patientId: " + patientId);
            return null;
        }
        SensorData lastValue = dataList.get(dataList.size() - 1);
        logger.log("info", "Last value for patientId: " + patientId + " -> " + lastValue);
        return lastValue;
    }

    @Override
    public void clearValues(long patientId) {
        logger.log("info", "Clearing all values for patientId: " + patientId);
        saveToS3(patientId, new ArrayList<>());
    }

    @Override
    public void clearAndAddValue(long patientId, SensorData sensorData) {
        logger.log("info", "Clearing and adding new value for patientId: " + patientId + " -> " + sensorData);
        List<SensorData> dataList = new ArrayList<>();
        dataList.add(sensorData);
        saveToS3(patientId, dataList);
    }

    private void saveToS3(long patientId, List<SensorData> dataList) {
        try {
            logger.log("info", "Saving data to S3 for patientId: " + patientId + " -> " + dataList);
            String json = convertSensorDataListToJson(dataList);

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(getS3Key(patientId))
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromString(json));

            logger.log("info", "Successfully saved data to S3 for patientId: " + patientId);
        } catch (AwsServiceException | SdkClientException e) {
            logger.log("error", "Error saving data to S3 for patientId: " + patientId + " -> " + e.getMessage());
        }
    }

    private List<SensorData> parseJsonToSensorDataList(String json) {
        List<SensorData> sensorDataList = new ArrayList<>();
        try {
            logger.log("info", "Parsing JSON data...");
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                SensorData sensorData = new SensorData(
                        jsonObject.getLong("patientId"),
                        jsonObject.getInt("value"),
                        jsonObject.getLong("timestamp"));
                sensorDataList.add(sensorData);
            }
            logger.log("info", "Successfully parsed JSON data.");
        } catch (JSONException e) {
            logger.log("error", "Error parsing JSON data: " + e.getMessage());
        }
        return sensorDataList;
    }

    private String convertSensorDataListToJson(List<SensorData> dataList) {
        logger.log("info", "Converting data to JSON...");
        JSONArray jsonArray = new JSONArray();
        for (SensorData sensorData : dataList) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("patientId", sensorData.patientId());
            jsonObject.put("value", sensorData.value());
            jsonObject.put("timestamp", sensorData.timestamp());
            jsonArray.put(jsonObject);
        }
        logger.log("info", "Successfully converted data to JSON.");
        return jsonArray.toString();
    }
}