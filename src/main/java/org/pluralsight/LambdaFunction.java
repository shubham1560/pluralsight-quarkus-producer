package org.pluralsight;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.pluralsight.dto.OrderRequest;
import org.pluralsight.service.OrderProducerService;

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

            if ("POST".equals(httpMethod) && "/order/initiate".equals(path)) {
                OrderRequest orderRequest = objectMapper.readValue(body, OrderRequest.class);
                var result = orderProducerService.initiateOrder(orderRequest);

                response.setStatusCode(200);
                response.setBody(objectMapper.writeValueAsString(result));
                response.setHeaders(new java.util.HashMap<>());
                response.getHeaders().put("Content-Type", "application/json");
                response.getHeaders().put("Access-Control-Allow-Origin", "*");
                
            } else {
                response.setStatusCode(404);
                response.setBody("Not Found");
            }

        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("Error: " + e.getMessage());
            response.setHeaders(new java.util.HashMap<>());
            response.getHeaders().put("Content-Type", "application/json");
        }

        return response;
    }
}