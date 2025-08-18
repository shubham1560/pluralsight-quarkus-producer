package org.pluralsight.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.pluralsight.dto.OrderRequest;
import org.pluralsight.model.OrderInitiated;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Random;

@ApplicationScoped
public class OrderTransformationService {
    
    private static final Logger LOG = Logger.getLogger(OrderTransformationService.class);
    private final Random random = new Random();
    
    public OrderInitiated enrichOrder(OrderRequest request) {
        LOG.infof("Enriching order: %s", request.getOrderId());
        
        OrderInitiated enrichedOrder = new OrderInitiated();
        enrichedOrder.setOrderId(request.getOrderId());
        enrichedOrder.setCustomerName(request.getCustomerName());
        enrichedOrder.setProductName(request.getProductName());
        enrichedOrder.setQuantity(request.getQuantity());
        enrichedOrder.setTotalAmount(request.getTotalAmount());
        
        enrichedOrder.setDeliveryType(determineDeliveryType());
        enrichedOrder.setLiveInventoryCheck(checkInventoryAvailability(request.getProductName(), request.getQuantity()));
        
        LOG.infof("Order enriched with delivery type: %s, inventory: %s", 
                  enrichedOrder.getDeliveryType(), enrichedOrder.getLiveInventoryCheck());
        
        return enrichedOrder;
    }
    
    private String determineDeliveryType() {
        String[] deliveryOptions = {"SAME_DAY", "NEXT_DAY", "TWO_DAY", "STANDARD"};
        
        double randomValue = random.nextDouble();
        if (randomValue < 0.3) {
            return "SAME_DAY";
        } else if (randomValue < 0.6) {
            return "NEXT_DAY";
        } else if (randomValue < 0.8) {
            return "TWO_DAY";
        } else {
            return "STANDARD";
        }
    }
    
    private String checkInventoryAvailability(String productName, int quantity) {
        double randomValue = random.nextDouble();
        
        if (productName.toLowerCase().contains("laptop") && randomValue < 0.4) {
            return "LOW_STOCK";
        } else if (randomValue < 0.8) {
            return "AVAILABLE";
        } else {
            return "NOT_AVAILABLE";
        }
    }
    
    public OrderInitiated transformOrder(OrderRequest request) {
        LOG.infof("Starting transformation pipeline for order: %s", request.getOrderId());
        
        OrderInitiated transformedOrder = enrichOrder(request);
        
        LOG.infof("Transformation pipeline completed for order: %s. Delivery: %s, Inventory: %s", 
                  transformedOrder.getOrderId(), 
                  transformedOrder.getDeliveryType(), 
                  transformedOrder.getLiveInventoryCheck());
        
        return transformedOrder;
    }
}
