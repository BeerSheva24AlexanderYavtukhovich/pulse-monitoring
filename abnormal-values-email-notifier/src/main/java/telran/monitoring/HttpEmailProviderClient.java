package telran.monitoring;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONObject;


public class HttpEmailProviderClient implements EmailProviderClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String baseUrl;

    public HttpEmailProviderClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public String getEmail(long patientId) {
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
                    String email = json.getString("email");
                    return email;
                }
                case 404 -> throw new RuntimeException("Email not found for patient ID: " + patientId);
                default -> throw new RuntimeException("Error while getting email: " + response.statusCode());
            }
        } catch (IOException | InterruptedException | RuntimeException e) {
            throw new RuntimeException("Failed to get email ", e);
        }
    }
}

