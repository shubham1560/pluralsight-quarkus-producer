package org.pluralsight.service;

import jakarta.enterprise.context.ApplicationScoped;
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
    
    private final OrderTransformationService orderTransformationService;

    public OrderProducerService() {
        this.orderTransformationService = new OrderTransformationService();
    }

    public OrderInitiated initiateOrder(OrderRequest orderRequest) {
        OrderInitiated transformedOrder = orderTransformationService.transformOrder(orderRequest);

         try {
            String orderJson = objectWriter.writeValueAsString(transformedOrder);
            LOG.infof("Order would be sent to Kafka: %s", orderJson);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to serialize order to JSON: %s", transformedOrder.getOrderId());
        }

        return transformedOrder;
    }
}
