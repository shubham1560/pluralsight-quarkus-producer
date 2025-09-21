package org.pluralsight.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.pluralsight.model.OrderInitiated;
import org.pluralsight.dto.OrderRequest;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import java.util.Properties;

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
    
    private void sendToKafka(String orderJson) {
        try {
            // Get MSK bootstrap servers from environment variable
            String bootstrapServers = System.getenv("MSK_BROKERS");
            if (bootstrapServers == null) {
                bootstrapServers = "boot-zosnm4je.c3.kafka-serverless.ap-south-1.amazonaws.com:9098";
            }
            
            LOG.infof("Connecting to MSK at: %s", bootstrapServers);
            
            // Create Kafka producer properties
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            props.put(ProducerConfig.ACKS_CONFIG, "all");
            props.put(ProducerConfig.RETRIES_CONFIG, 3);
            props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
            props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
            props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
            
            // MSK IAM Authentication
            props.put("security.protocol", "SASL_SSL");
            props.put("sasl.mechanism", "AWS_MSK_IAM");
            props.put("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;");
            props.put("sasl.client.callback.handler.class", "software.amazon.msk.auth.iam.IAMClientCallbackHandler");
            
            // Create producer and send message
            try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
                ProducerRecord<String, String> record = new ProducerRecord<>("order-initiated", orderJson);
                
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        LOG.errorf(exception, "Failed to send message to Kafka topic: order-initiated");
                    } else {
                        LOG.infof("Successfully sent message to Kafka topic: %s, partition: %d, offset: %d", 
                                metadata.topic(), metadata.partition(), metadata.offset());
                    }
                });
                
                producer.flush();
                LOG.infof("Message sent to Kafka: %s", orderJson);
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send message to Kafka");
        }
    }

    public OrderInitiated initiateOrder(OrderRequest orderRequest) {
        LOG.infof("Initiating new order: %s", orderRequest);

        OrderInitiated transformedOrder = orderTransformationService.transformOrder(orderRequest);

        LOG.infof("Order initiated: %s", transformedOrder);

        // Convert order to JSON string and send to Kafka
        try {
            String orderJson = objectWriter.writeValueAsString(transformedOrder);
            LOG.infof("Sending order initiated event to Kafka: %s", orderJson);
            
            sendToKafka(orderJson);
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to serialize order to JSON: %s", transformedOrder.getOrderId());
        }

        return transformedOrder;
    }
}
