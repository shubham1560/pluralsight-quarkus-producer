package org.pluralsight.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.pluralsight.model.OrderInitiated;
import org.pluralsight.dto.OrderRequest;
import org.pluralsight.service.OrderProducerService;
import org.jboss.logging.Logger;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderProducerResource {
    
    private static final Logger LOG = Logger.getLogger(OrderProducerResource.class);
    
    @Inject
    OrderProducerService orderProducerService;
    
    @POST
    @Path("/initiate")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response initiateOrder(OrderRequest orderRequest) {
        
        try {
            LOG.infof("Initiating order: %s", orderRequest);
            
            OrderInitiated initiatedOrder = orderProducerService.initiateOrder(orderRequest);
            
            LOG.infof("Order initiated successfully: %s", initiatedOrder.getOrderId());
            
            return Response.status(Response.Status.CREATED)
                    .entity(initiatedOrder)
                    .build();
                    
        } catch (Exception e) {
            LOG.errorf(e, "Error initiating order");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error initiating order: " + e.getMessage())
                    .build();
        }
    }
}
