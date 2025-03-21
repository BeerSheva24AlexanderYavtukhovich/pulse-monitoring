package telran.monitoring;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import telran.monitoring.api.Range;

public class AppRange implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final RangeDataSource rangeDataSource;

    public AppRange() {
        this.rangeDataSource = new RangeDataSource();
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
            Range range = rangeDataSource.getRange(patientId);
            String responseBody = String.format("{\"min\":%d, \"max\":%d}", range.min(), range.max());
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