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

@ApplicationScoped
public class OrderProducerService {
    
    private static final Logger LOG = Logger.getLogger(OrderProducerService.class);
    private static final ObjectWriter objectWriter = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .writer();
    
    @Inject
    @Channel("order-initiated")
    Emitter<String> orderInitiatedEmitter;
    
    public OrderInitiated initiateOrder(OrderRequest orderRequest) {
        OrderInitiated order = new OrderInitiated();
        order.setOrderId(orderRequest.getOrderId());
        order.setCustomerName(orderRequest.getCustomerName());
        order.setProductName(orderRequest.getProductName());
        order.setQuantity(orderRequest.getQuantity());
        order.setTotalAmount(orderRequest.getTotalAmount());
        
        LOG.infof("Initiating new order: %s", order);
        
        // Convert order to JSON string and send to Kafka
        try {
            String orderJson = objectWriter.writeValueAsString(order);
            orderInitiatedEmitter.send(orderJson)
                .whenComplete((success, failure) -> {
                    if (failure != null) {
                        LOG.errorf(failure, "Failed to send order initiated event to Kafka: %s", order.getOrderId());
                    } else {
                        LOG.infof("Successfully sent order initiated event to Kafka: %s", order.getOrderId());
                    }
                });
        } catch (Exception e) {
            LOG.errorf(e, "Failed to serialize order to JSON: %s", order.getOrderId());
        }
        
        return order;
    }
}
