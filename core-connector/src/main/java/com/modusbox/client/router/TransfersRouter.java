package com.modusbox.client.router;

import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.json.JSONException;

public class TransfersRouter extends RouteBuilder {

    private final RouteExceptionHandlingConfigurer exception = new RouteExceptionHandlingConfigurer();

    private static final String ROUTE_ID = "com.modusbox.postTransfers";
    private static final String ROUTE_ID_PUT = "com.modusbox.putTransfersByTransferId";
    private static final String ROUTE_ID_GET = "com.modusbox.getTransfersByTransferId";
    private static final String COUNTER_NAME = "counter_post_transfers_requests";
    private static final String COUNTER_NAME_PUT = "counter_put_transfers_requests";
    private static final String COUNTER_NAME_GET = "counter_get_transfers_requests";
    private static final String TIMER_NAME = "histogram_post_transfers_timer";
    private static final String TIMER_NAME_PUT = "histogram_put_transfers_timer";
    private static final String TIMER_NAME_GET = "histogram_get_transfers_timer";
    private static final String HISTOGRAM_NAME = "histogram_post_transfers_requests_latency";
    private static final String HISTOGRAM_NAME_PUT = "histogram_put_transfers_requests_latency";
    private static final String HISTOGRAM_NAME_GET = "histogram_get_transfers_requests_latency";

    public static final Counter requestCounter = Counter.build()
            .name(COUNTER_NAME)
            .help("Total requests for POST /transfers.")
            .register();

    private static final Histogram requestLatency = Histogram.build()
            .name(HISTOGRAM_NAME)
            .help("Request latency in seconds for POST /transfers.")
            .register();

    public static final Counter requestCounterPut = Counter.build()
            .name(COUNTER_NAME_PUT)
            .help("Total requests for PUT /transfers/{transferId}.")
            .register();

    private static final Histogram requestLatencyPut = Histogram.build()
            .name(HISTOGRAM_NAME_PUT)
            .help("Request latency in seconds for PUT /transfers/{transferId}.")
            .register();

    public static final Counter requestCounterGet = Counter.build()
            .name(COUNTER_NAME_GET)
            .help("Total requests for GET /transfers/{transferId}.")
            .register();

    private static final Histogram requestLatencyGet = Histogram.build()
            .name(HISTOGRAM_NAME_GET)
            .help("Request latency in seconds for GET /transfers/{transferId}.")
            .register();

    public void configure() {

        // Add custom global exception handling strategy
        exception.configureExceptionHandling(this);

        from("direct:postTransfers").routeId(ROUTE_ID).doTry()
                .process(exchange -> {
                    requestCounter.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME, requestLatency.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, " + ROUTE_ID + "', null, null, 'Input Payload: ${body}')") // default logging
                /*
                 * BEGIN processing
                 */
//                 .setProperty("origPayload", simple("${body}"))
//                 .to("direct:getAuthHeader")

//                 .marshal().json()
//                 .transform(datasonnet("resource:classpath:mappings/postTransactionRequest.ds"))
//                 .setBody(simple("${body.content}"))
//                 .marshal().json()

//                 .removeHeaders("CamelHttp*")
//                 .setHeader(Exchange.HTTP_METHOD, constant("POST"))
//                 .setHeader("Content-Type", constant("application/json"))
//                 .setHeader("Accept", constant("application/json"))
//                 .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
//                         "'Calling backend API, postTransfers, POST {{dfsp.host}}', " +
//                         "'Tracking the request', 'Track the response', 'Input Payload: ${body}')")

//                 .toD("{{dfsp.host}}/okdollar/v1/Payment?bridgeEndpoint=true&throwExceptionOnFailure=false")
//                 .unmarshal().json(JsonLibrary.Gson)
//                 .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
//                         "'Response from backend API, postTransfers: ${body}', " +
//                         "'Tracking the response', 'Verify the response', null)")
                //.setBody(constant("{\"homeTransactionId\": \"1234\"}"))

                .setProperty("origPayload", simple("${body}"))
                .to("direct:getAuthHeader")
                .setHeader("token", simple("${exchangeProperty.token}"))

                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/postTransfersRequest.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()

                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))

                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Calling backend API, post transfers, POST {{dfsp.host}}', " +
                        "'Tracking the request', 'Track the response', 'Input Payload: ${body}')")
                .toD("{{dfsp.host}}/okdollar/v1/Payment?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .unmarshal().json(JsonLibrary.Gson)
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from backend API, post transfers: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")

                .choice()
                    .when(simple("${body['code']} != 200"))
                        .to("direct:catchCBSError")
                .endDoTry()

                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/postTransfersResponse.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json(JsonLibrary.Gson)

                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Final Response: ${body}', " +
                        "null, null, 'Response of post /transfers API')")
                .doCatch(CCCustomException.class, HttpOperationFailedException.class, JSONException.class)
                    .to("direct:extractCustomErrors")
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

        from("direct:putTransfersByTransferId").routeId(ROUTE_ID_PUT).doTry()
                .process(exchange -> {
                    requestCounterPut.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_PUT, requestLatencyPut.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, PUT /transfers/${header.transferId}', " +
                        "null, null, 'Input Payload: ${body}')")
                /*
                 * BEGIN processing
                 */

                .setProperty("origPayload", simple("${body}"))
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                .setBody(constant(""))
//                .setProperty("origPayload", simple("${body}"))
//                .to("direct:getAuthHeader")
//                .setHeader("token", simple("${exchangeProperty.token}"))

//                .marshal().json()
//                .transform(datasonnet("resource:classpath:mappings/putTransactionRequest.ds"))
//                .setBody(constant(""))
//                .marshal().json()
//
//                .removeHeaders("CamelHttp*")
//                .setHeader("Content-Type", constant("application/json"))
//                .setHeader("Accept", constant("application/json"))
//                .setHeader(Exchange.HTTP_METHOD, constant("POST"))

//                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
//                        "'Calling backend API, put transfers, POST {{dfsp.host}}', " +
//                        "'Tracking the request', 'Track the response', 'Input Payload: ${body}')")
//                .toD("{{dfsp.host}}/okdollar/v1/Payment?bridgeEndpoint=true&throwExceptionOnFailure=false")
//                .unmarshal().json(JsonLibrary.Gson)
//                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
//                        "'Response from backend API, put transfers: ${body}', " +
//                        "'Tracking the response', 'Verify the response', null)")
////                .process(exchange -> System.out.println())
//                .choice()
//                    .when(simple("${body['code']} != 200"))
//                        .to("direct:catchCBSError")
//                .endDoTry()
//
//                .marshal().json()
//                .transform(datasonnet("resource:classpath:mappings/putTransactionResponse.ds"))
//                .setBody(simple("${body.content}"))
//                .marshal().json(JsonLibrary.Gson)

                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Final Response: ${body}', " +
                        "null, null, 'Response of PUT /transfers/${header.transferId} API')")
                .doCatch(CCCustomException.class, HttpOperationFailedException.class, JSONException.class)
                    .to("direct:extractCustomErrors")
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_PUT)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

        from("direct:getTransfersByTransferId").routeId(ROUTE_ID_GET).doTry()
                .process(exchange -> {
                    requestCounterGet.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_GET, requestLatencyGet.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, GET /transfers/${header.transferId}', " +
                        "null, null, null)")
                /*
                 * BEGIN processing
                 */

                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))

                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Calling Hub API, get transfers, GET {{ml-conn.outbound.host}}', " +
                        "'Tracking the request', 'Track the response', 'Input Payload: ${body}')")
                .toD("{{ml-conn.outbound.host}}/transfers/${header.transferId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Hub API, get transfers: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")
                .unmarshal().json(JsonLibrary.Gson)
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Hub API, get transfers: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")
//                .process(exchange -> System.out.println())

                .choice()
                .when(simple("${body['statusCode']} != null"))
//                .process(exchange -> System.out.println())
                    .to("direct:catchMojaloopError")
                .endDoTry()

//                .process(exchange -> System.out.println())

                .choice()
                .when(simple("${body['fulfil']} != null"))
//                .process(exchange -> System.out.println())            
                    .marshal().json()
                    .transform(datasonnet("resource:classpath:mappings/getTransfersResponse.ds"))
                    .setBody(simple("${body.content}"))
                    .marshal().json()
                .endDoTry()

                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Final Response: ${body}', " +
                        "null, null, 'Response of GET /transfers/${header.transferId} API')")

                .doCatch(CCCustomException.class, HttpOperationFailedException.class, JSONException.class)
                    .to("direct:extractCustomErrors")
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_GET)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

    }
}
