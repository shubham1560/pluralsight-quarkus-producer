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
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.ListTopicsResult;
import java.util.Properties;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class OrderProducerService {
    
    private static final Logger LOG = Logger.getLogger(OrderProducerService.class);
    private static final ObjectWriter objectWriter = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .writer();
    
    @Inject
    private OrderTransformationService orderTransformationService;
    
    @Inject
    @Channel("order-initiated")
    Emitter<String> orderInitiatedEmitter;
    
    // Fallback for Lambda environment where CDI might not work
    private OrderTransformationService getOrderTransformationService() {
        if (orderTransformationService == null) {
            LOG.warn("CDI injection failed, using manual instantiation");
            return new OrderTransformationService();
        }
        return orderTransformationService;
    }
    
    private void sendToKafka(String orderJson) {
        if (orderInitiatedEmitter == null) {
            LOG.warn("CDI injection failed for Emitter, using direct Kafka producer");
            sendToKafkaDirect(orderJson);
        } else {
            orderInitiatedEmitter.send(orderJson)
                .whenComplete((success, failure) -> {
                    if (failure != null) {
                        LOG.errorf(failure, "Failed to send order initiated event to Kafka");
                    } else {
                        LOG.infof("Successfully sent order initiated event to Kafka");
                    }
                });
        }
    }
    
    private void sendToKafkaDirect(String orderJson) {
        try {
            // Get MSK bootstrap servers from environment variable
            String bootstrapServers = System.getenv("MSK_BROKERS");
            if (bootstrapServers == null) {
                bootstrapServers = "boot-zosnm4je.c3.kafka-serverless.ap-south-1.amazonaws.com:9098";
            }
            
            LOG.infof("Connecting to MSK at: %s", bootstrapServers);
            
            // Ensure topic exists
            ensureTopicExists(bootstrapServers);
            
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
            LOG.errorf(e, "Failed to send message to Kafka using direct producer");
        }
    }
    
    private void ensureTopicExists(String bootstrapServers) {
        try {
            // Create admin client properties
            Properties adminProps = new Properties();
            adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            adminProps.put("security.protocol", "SASL_SSL");
            adminProps.put("sasl.mechanism", "AWS_MSK_IAM");
            adminProps.put("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;");
            adminProps.put("sasl.client.callback.handler.class", "software.amazon.msk.auth.iam.IAMClientCallbackHandler");
            
            try (AdminClient adminClient = AdminClient.create(adminProps)) {
                // Check if topic exists
                Set<String> existingTopics = adminClient.listTopics().names().get();
                
                if (!existingTopics.contains("order-initiated")) {
                    LOG.infof("Topic 'order-initiated' does not exist, creating it...");
                    
                    // Create the topic
                    NewTopic newTopic = new NewTopic("order-initiated", 3, (short) 3);
                    adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
                    
                    LOG.infof("Topic 'order-initiated' created successfully!");
                } else {
                    LOG.infof("Topic 'order-initiated' already exists");
                }
            }
        } catch (Exception e) {
            LOG.warnf(e, "Failed to ensure topic exists, continuing with message send");
        }
    }

    public OrderInitiated initiateOrder(OrderRequest orderRequest) {
        LOG.infof("Initiating new order: %s", orderRequest);

        OrderInitiated transformedOrder = getOrderTransformationService().transformOrder(orderRequest);

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
