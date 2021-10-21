# okdollar-pm4ml-core-connector

Most of the content details and instructions about development and deployment can be found into
the main [template project](https://github.com/pm4ml/template-rest-pm4ml-core-connector).
Additional or different topics specified below.

### Overwrite application properties

To run application and specify the proper credentials for DFSP API connection
(it is not required specify all the fields if it isn't used):
```
java \
-Dml-conn.outbound.host="http://localhost:4001" \
-Ddfsp.name="DFSP CO. LTD." \
-Ddfsp.host="https://dfsp/api" \
-Ddfsp.username="user" \
-Ddfsp.password="pass" \
-Ddfsp.projectid="P1" \
-Ddfsp.bankaccountnumber="123" \
-Ddfsp.channel="0" \
-jar ./core-connector/target/core-connector.jar
```
```
docker run --rm \
-e MLCONN_OUTBOUND_ENDPOINT="http://localhost:4001" \
-e DFSP_NAME="DFSP CO. LTD." \
-e DFSP_HOST="https://dfsp/api" \
-e DFSP_USERNAME="user" \
-e DFSP_PASSWORD="P\@ss0rd" \
-e DFSP_PROJECT_ID="P1" \
-e DFSP_BANKACCOUNTNUMBER="34878539475" \
-e DFSP_CHANNEL="0" \
-p 3003:3003 core-connector:latest
```
**NOTE:** keep the values in double quotes (") and scape any special character (\\@).

Additionally, to generate the Java Rest DSL router and Model files (In parent pom): mvn clean install

To Build the Project: mvn clean package

To Build the project using Docker: docker build -t okdollar-pm4ml-core-connector .

To run the project using Docker: docker run -p 3001:3001 -p 8080:8080 -t okdollar-pm4ml-core-connector

To run the Integration Tests (run mvn clean install under core-connector folder first): mvn -P docker-it clean install

Architecture diagram:

![Alt text](diagram.jpg?raw=true "Integration Architecture")