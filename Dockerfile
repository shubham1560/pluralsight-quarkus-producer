FROM public.ecr.aws/lambda/java:17

# Install unzip
RUN yum update -y && yum install -y unzip

# Copy the Quarkus Lambda function and extract it
COPY --chown=sbx_user1051:root target/function.zip ${LAMBDA_TASK_ROOT}/
RUN cd ${LAMBDA_TASK_ROOT} && unzip function.zip && rm function.zip

# Set the handler to use our custom Lambda function
CMD ["org.pluralsight.LambdaFunction::handleRequest"]
