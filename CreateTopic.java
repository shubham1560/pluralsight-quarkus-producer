import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.ConfigResource;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class CreateTopic {
    public static void main(String[] args) {
        // Kafka client properties for MSK Serverless
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "boot-zosnm4je.c3.kafka-serverless.ap-south-1.amazonaws.com:9098");
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "AWS_MSK_IAM");
        props.put("sasl.jaas.config", "software.amazon.msk.auth.iam.IAMLoginModule required;");
        props.put("sasl.client.callback.handler.class", "software.amazon.msk.auth.iam.IAMClientCallbackHandler");

        try (AdminClient adminClient = AdminClient.create(props)) {
            // Create the topic
            NewTopic newTopic = new NewTopic("order-initiated", 3, (short) 3);
            
            System.out.println("Creating topic: order-initiated");
            adminClient.createTopics(Collections.singletonList(newTopic)).all().get();
            System.out.println("Topic 'order-initiated' created successfully!");
            
            // Verify the topic was created
            boolean topicExists = adminClient.listTopics().names().get().contains("order-initiated");
            System.out.println("Topic exists: " + topicExists);
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Error creating topic: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
