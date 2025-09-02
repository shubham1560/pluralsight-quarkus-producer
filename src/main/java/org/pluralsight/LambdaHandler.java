package org.pluralsight;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.pluralsight.dto.OrderRequest;
import org.pluralsight.service.OrderProducerService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class LambdaHandler implements RequestStreamHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OrderProducerService orderProducerService = new OrderProducerService();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        try {
            // Parse the input
            Map<String, Object> input = objectMapper.readValue(inputStream, Map.class);

            // Extract HTTP method and path
            String httpMethod = (String) input.get("httpMethod");
            String path = (String) input.get("path");
            String body = (String) input.get("body");

            // Handle the order initiation
            if ("POST".equals(httpMethod) && "/orders/initiate".equals(path)) {
                // Parse the order request
                OrderRequest orderRequest = objectMapper.readValue(body, OrderRequest.class);

                // Process the order
                var result = orderProducerService.initiateOrder(orderRequest);

                // Create success response
                ObjectNode response = objectMapper.createObjectNode();
                response.put("statusCode", 200);
                
                // Add CORS headers
                ObjectNode headers = objectMapper.createObjectNode();
                headers.put("Content-Type", "application/json");
                headers.put("Access-Control-Allow-Origin", "*");
                headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
                response.set("headers", headers);
                
                response.put("body", objectMapper.writeValueAsString(result));

                // Write response
                objectMapper.writeValue(outputStream, response);
            } else if ("OPTIONS".equals(httpMethod)) {
                // Handle CORS preflight requests
                ObjectNode response = objectMapper.createObjectNode();
                response.put("statusCode", 200);
                
                ObjectNode headers = objectMapper.createObjectNode();
                headers.put("Access-Control-Allow-Origin", "*");
                headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
                headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
                response.set("headers", headers);
                
                response.put("body", "");

                objectMapper.writeValue(outputStream, response);
            } else {
                // Handle unsupported methods/paths
                ObjectNode response = objectMapper.createObjectNode();
                response.put("statusCode", 400);
                
                ObjectNode headers = objectMapper.createObjectNode();
                headers.put("Content-Type", "application/json");
                response.set("headers", headers);
                
                response.put("body", "Unsupported method or path");

                objectMapper.writeValue(outputStream, response);
            }

        } catch (Exception e) {
            // Handle errors
            ObjectNode response = objectMapper.createObjectNode();
            response.put("statusCode", 500);
            
            ObjectNode headers = objectMapper.createObjectNode();
            headers.put("Content-Type", "application/json");
            response.set("headers", headers);
            
            response.put("body", "Error: " + e.getMessage());
            objectMapper.writeValue(outputStream, response);
        }
    }
}
