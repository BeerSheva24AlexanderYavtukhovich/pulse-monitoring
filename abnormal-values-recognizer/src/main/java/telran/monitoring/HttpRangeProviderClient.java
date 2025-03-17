package telran.monitoring;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;

import telran.monitoring.api.Range;

public class HttpRangeProviderClient implements RangeProviderClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String baseUrl;

    public HttpRangeProviderClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public Range getRange(long patientId) {
        try {
            URI uri = URI.create(baseUrl + "?id=" + patientId);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            switch (response.statusCode()) {
                case 200 -> {
                    JSONObject json = new JSONObject(response.body());
                    int min = json.getInt("min");
                    int max = json.getInt("max");
                    return new Range(min, max);
                }
                case 404 -> throw new RuntimeException("Range not found for patient ID: " + patientId);
                default -> throw new RuntimeException("Error while fetching range: " + response.statusCode());
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            throw new RuntimeException("Failed to fetch range ", e);
        }
    }
}

