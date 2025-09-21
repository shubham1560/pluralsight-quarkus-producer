# Producer Lambda with Kafka Integration Deployment Guide

This guide provides step-by-step instructions for deploying the Quarkus producer application to AWS Lambda with full Kafka (MSK) integration to send messages to the `order-initiated` topic.

## Prerequisites

- AWS CLI configured with appropriate permissions
- Docker installed and running
- Maven installed
- Java 17 installed
- AWS Account with MSK cluster set up
- Existing Lambda function `quarkus-producer` (or create new one)

## Architecture Overview

```
API Gateway → Lambda Producer → Kafka/MSK Topic (order-initiated) → Lambda Consumer
```

## Step 1: Build the Application

### 1.1 Navigate to the producer project
```bash
cd kafka-quarkus
```

### 1.2 Build the Quarkus application
```bash
mvn clean package -DskipTests
```

This will create:
- `target/function.zip` - Lambda deployment package
- `target/kafka-quarkus-1.0.0-SNAPSHOT-runner.jar` - JAR file

## Step 2: Create/Update ECR Repository

### 2.1 Create ECR repository for the producer (if not exists)
```bash
aws ecr create-repository \
    --repository-name quarkus-producer \
    --region ap-south-1
```

### 2.2 Get the ECR login token
```bash
aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin 494077377285.dkr.ecr.ap-south-1.amazonaws.com
```

## Step 3: Build and Push Docker Image

### 3.1 Build the Docker image
```bash
docker buildx build --platform linux/amd64 -t quarkus-producer .
```

### 3.2 Tag the image for ECR
```bash
docker tag quarkus-producer:latest 494077377285.dkr.ecr.ap-south-1.amazonaws.com/quarkus-producer:latest
```

### 3.3 Push the image to ECR
```bash
docker push 494077377285.dkr.ecr.ap-south-1.amazonaws.com/quarkus-producer:latest
```

## Step 4: Update IAM Role for Lambda (if needed)

### 4.1 Create MSK policy for producer (if not exists)
```bash
aws iam create-policy \
    --policy-name MSKProducerPolicy \
    --policy-document file://msk-policy.json
```

### 4.2 Attach MSK policy to existing Lambda role
```bash
aws iam attach-role-policy \
    --role-name lambda-execution-role \
    --policy-arn arn:aws:iam::494077377285:policy/MSKProducerPolicy
```

## Step 5: Update Lambda Function

### 5.1 Update the Lambda function with new image
```bash
aws lambda update-function-code \
    --function-name quarkus-producer \
    --image-uri 494077377285.dkr.ecr.ap-south-1.amazonaws.com/quarkus-producer:latest \
    --region ap-south-1
```

### 5.2 Configure VPC settings (required for MSK access)
```bash
aws lambda update-function-configuration \
    --function-name quarkus-producer \
    --vpc-config SubnetIds=subnet-8a5f56e2,subnet-cf46c7b4,subnet-ea8ef1a6,SecurityGroupIds=sg-0123456789abcdef0 \
    --region ap-south-1
```

### 5.3 Set environment variables for MSK
```bash
aws lambda update-function-configuration \
    --function-name quarkus-producer \
    --environment Variables='{
        "MSK_BROKERS": "boot-zosnm4je.c3.kafka-serverless.ap-south-1.amazonaws.com:9098"
    }' \
    --region ap-south-1
```

### 5.4 Update timeout and memory (if needed)
```bash
aws lambda update-function-configuration \
    --function-name quarkus-producer \
    --timeout 30 \
    --memory-size 512 \
    --region ap-south-1
```

## Step 6: Test the Kafka Integration

### 6.1 Test the API Gateway endpoint
```bash
curl -X POST \
  https://lfrnc1p0d5.execute-api.ap-south-1.amazonaws.com/prod/order/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "TEST-KAFKA-001",
    "customerName": "John Doe",
    "productName": "Laptop",
    "totalAmount": 999.99
  }'
```

### 6.2 Check Lambda logs for Kafka sending
```bash
aws logs filter-log-events \
    --log-group-name /aws/lambda/quarkus-producer \
    --start-time $(date -d '5 minutes ago' +%s)000
```

### 6.3 Verify message in MSK (if consumer is deployed)
```bash
aws logs filter-log-events \
    --log-group-name /aws/lambda/quarkus-consumer \
    --start-time $(date -d '2 minutes ago' +%s)000
```

## Step 7: Create Kafka Topic (if needed)

### 7.1 Create topic using AWS CLI
```bash
aws kafka create-topic \
    --cluster-arn arn:aws:kafka:ap-south-1:494077377285:cluster/demo-cluster-1/429c9e01-d348-4915-9f94-e8b827997c9f-s3 \
    --topic-name order-initiated \
    --partitions 3 \
    --replication-factor 3
```

## Configuration Details

### Application Properties
The producer is configured with:
- MSK bootstrap servers via environment variable
- IAM authentication for MSK
- SmallRye Kafka connector for reactive messaging
- Fallback to direct Kafka producer for Lambda environments

### Dual Mode Operation
The `OrderProducerService` supports:
1. **CDI Mode**: Uses `@Inject` and `@Channel` for reactive messaging
2. **Direct Mode**: Uses direct Kafka producer when CDI fails in Lambda

### MSK IAM Authentication
- Security Protocol: `SASL_SSL`
- SASL Mechanism: `AWS_MSK_IAM`
- JAAS Config: `software.amazon.msk.auth.iam.IAMLoginModule required`

## Troubleshooting

### Common Issues

1. **VPC Configuration**: Ensure Lambda is in the same VPC as MSK
2. **Security Groups**: Allow outbound traffic on port 9098
3. **IAM Permissions**: Verify MSK write permissions
4. **Environment Variables**: Check `MSK_BROKERS` is set correctly

### Monitoring

- CloudWatch Logs: `/aws/lambda/quarkus-producer`
- Look for logs: "Successfully sent message to Kafka topic"
- Check for errors: "Failed to send message to Kafka"

### Debug Commands

```bash
# Check Lambda function configuration
aws lambda get-function-configuration --function-name quarkus-producer

# Check VPC configuration
aws lambda get-function-configuration --function-name quarkus-producer --query 'VpcConfig'

# Check environment variables
aws lambda get-function-configuration --function-name quarkus-producer --query 'Environment'
```

## Expected Log Output

When working correctly, you should see logs like:
```
INFO: Initiating new order: OrderRequest{orderId='TEST-KAFKA-001', ...}
INFO: Order initiated: OrderInitiated{orderId='TEST-KAFKA-001', ...}
INFO: Sending order initiated event to Kafka: {"orderId":"TEST-KAFKA-001",...}
INFO: Connecting to MSK at: boot-zosnm4je.c3.kafka-serverless.ap-south-1.amazonaws.com:9098
INFO: Successfully sent message to Kafka topic: order-initiated, partition: 0, offset: 123
INFO: Message sent to Kafka: {"orderId":"TEST-KAFKA-001",...}
```

## Next Steps

1. **Deploy Consumer**: Use the consumer deployment guide to process messages
2. **Monitoring**: Set up CloudWatch alarms for Kafka send failures
3. **Scaling**: Configure Lambda concurrency for high throughput
4. **Error Handling**: Implement retry logic and dead letter queues

## Cleanup

To remove Kafka integration:
```bash
# Detach MSK policy
aws iam detach-role-policy \
    --role-name lambda-execution-role \
    --policy-arn arn:aws:iam::494077377285:policy/MSKProducerPolicy

# Delete MSK policy
aws iam delete-policy --policy-arn arn:aws:iam::494077377285:policy/MSKProducerPolicy
```

