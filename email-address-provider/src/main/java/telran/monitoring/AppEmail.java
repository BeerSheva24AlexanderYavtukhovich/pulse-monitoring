package telran.monitoring;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class AppEmail implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final EmailDataSource emailDataSource;

    public AppEmail() {
        this.emailDataSource = new EmailDataSource();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);
        try {
            Map<String, String> queryParams = input.getQueryStringParameters();
            if (queryParams == null || !queryParams.containsKey("id")) {
                throw new IllegalArgumentException("Parameter 'id' is required.");
            }
            long patientId = Long.parseLong(queryParams.get("id"));
            String email = emailDataSource.getEmail(patientId);
            String responseBody = String.format("{\"email\":%s}", email);
            return response.withStatusCode(200).withBody(responseBody);

        } catch (NoSuchElementException e) {
            return response.withStatusCode(404).withBody(e.getMessage());
        } catch (IllegalArgumentException e) {
            return response.withStatusCode(400).withBody(e.getMessage());
        } catch (Exception e) {
            return response.withStatusCode(500).withBody(e.getMessage());
        }
    }
}