package org.pluralsight.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.pluralsight.model.OrderInitiated;
import org.pluralsight.dto.OrderRequest;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class OrderProducerService {
    
    private static final Logger LOG = Logger.getLogger(OrderProducerService.class);
    private static final ObjectWriter objectWriter = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .writer();
    
    private final OrderTransformationService orderTransformationService;

    public OrderProducerService() {
        this.orderTransformationService = new OrderTransformationService();
    }

    public OrderInitiated initiateOrder(OrderRequest orderRequest) {
        LOG.infof("Initiating new order: %s", orderRequest);

        OrderInitiated transformedOrder = orderTransformationService.transformOrder(orderRequest);

        LOG.infof("Order initiated: %s", transformedOrder);

        // TODO: Convert order to JSON string and send to Kafka
        // Commented out for Lambda deployment without Kafka
        /*
        try {
            String orderJson = objectWriter.writeValueAsString(transformedOrder);
            orderInitiatedEmitter.send(orderJson)
                .whenComplete((success, failure) -> {
                    if (failure != null) {
                        LOG.errorf(failure, "Failed to send order initiated event to Kafka: %s", transformedOrder.getOrderId());
                    } else {
                        LOG.infof("Successfully sent order initiated event to Kafka: %s", transformedOrder.getOrderId());
                    }
                });
        } catch (Exception e) {
            LOG.errorf(e, "Failed to serialize order to JSON: %s", transformedOrder.getOrderId());
        }
        */
        
        // For now, just log the order that would be sent to Kafka
        try {
            String orderJson = objectWriter.writeValueAsString(transformedOrder);
            LOG.infof("Order would be sent to Kafka: %s", orderJson);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to serialize order to JSON: %s", transformedOrder.getOrderId());
        }

        return transformedOrder;
    }
}
