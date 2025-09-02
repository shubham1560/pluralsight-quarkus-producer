package org.pluralsight;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pluralsight.dto.OrderRequest;
import org.pluralsight.service.OrderProducerService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

public class LambdaFunction implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final OrderProducerService orderProducerService;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public LambdaFunction() {
        this.orderProducerService = new OrderProducerService();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        
        try {
            String httpMethod = input.getHttpMethod();
            String path = input.getPath();
            String body = input.getBody();

            // Handle the order initiation
            if ("POST".equals(httpMethod) && "/orders/initiate".equals(path)) {
                // Parse the order request
                OrderRequest orderRequest = objectMapper.readValue(body, OrderRequest.class);

                // Process the order
                var result = orderProducerService.initiateOrder(orderRequest);

                // Create success response
                response.setStatusCode(200);
                response.setBody(objectMapper.writeValueAsString(result));
                
                // Add CORS headers
                response.setHeaders(new java.util.HashMap<>());
                response.getHeaders().put("Content-Type", "application/json");
                response.getHeaders().put("Access-Control-Allow-Origin", "*");
                response.getHeaders().put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                response.getHeaders().put("Access-Control-Allow-Headers", "Content-Type, Authorization");
                
            } else if ("OPTIONS".equals(httpMethod)) {
                // Handle CORS preflight requests
                response.setStatusCode(200);
                response.setBody("");
                response.setHeaders(new java.util.HashMap<>());
                response.getHeaders().put("Access-Control-Allow-Origin", "*");
                response.getHeaders().put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                response.getHeaders().put("Access-Control-Allow-Headers", "Content-Type, Authorization");
                
            } else {
                // Handle unsupported methods/paths
                response.setStatusCode(400);
                response.setBody("Unsupported method or path");
                response.setHeaders(new java.util.HashMap<>());
                response.getHeaders().put("Content-Type", "application/json");
            }

        } catch (Exception e) {
            // Handle errors
            response.setStatusCode(500);
            response.setBody("Error: " + e.getMessage());
            response.setHeaders(new java.util.HashMap<>());
            response.getHeaders().put("Content-Type", "application/json");
        }

        return response;
    }
}
