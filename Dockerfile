### Build runtime image
FROM openjdk:8-jdk-alpine

ARG JAR_FILE=core-connector/target/*.jar

COPY ${JAR_FILE} app.jar

ENV MLCONN_OUTBOUND_ENDPOINT=http://simulator:3004
ENV DFSP_NAME="DFSP CO. LTD."
ENV DFSP_HOST="https://localhost/api"
ENV DFSP_USERNAME=username
ENV DFSP_PASSWORD=password
ENV DFSP_PROJECT_ID=clientId
ENV DFSP_BANKACCOUNTNUMBER=bankaccountnumber
ENV DFSP_CHANNEL=0

ENTRYPOINT ["java", "-Dml-conn.outbound.host=${MLCONN_OUTBOUND_ENDPOINT}", "-Ddfsp.name=${DFSP_NAME}", "-Ddfsp.host=${DFSP_HOST}", "-Ddfsp.username=${DFSP_USERNAME}", "-Ddfsp.password=${DFSP_PASSWORD}", "-Ddfsp.projectid=${DFSP_PROJECTID}", "-Ddfsp.bankaccountnumber=${DFSP_BANKACCOUNTNUMBER}", "-Ddfsp.channel=${DFSP_CHANNEL}", "-jar", "/app.jar"]

EXPOSE 3003
