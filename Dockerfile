FROM public.ecr.aws/lambda/java:17

RUN yum update -y && yum install -y unzip

COPY --chown=sbx_user1051:root target/function.zip ${LAMBDA_TASK_ROOT}/
RUN cd ${LAMBDA_TASK_ROOT} && unzip function.zip && rm function.zip

CMD ["org.pluralsight.LambdaFunction::handleRequest"]