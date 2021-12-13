package com.modusbox.client.router;

import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.CorsFilter;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.json.JSONException;

public class PartiesRouter extends RouteBuilder {

	private final RouteExceptionHandlingConfigurer exception = new RouteExceptionHandlingConfigurer();
	private final CorsFilter corsFilter = new CorsFilter();

	// Prometheus metrics for GET /parties/{idType}/{idValue}
	private static final String ROUTE_ID = "com.modusbox.getPartiesByIdTypeIdValue";
	private static final String COUNTER_NAME = "counter_get_parties_requests";
	private static final String TIMER_NAME = "histogram_get_parties_timer";
	private static final String HISTOGRAM_NAME = "histogram_get_parties_requests_latency";

	public static final Counter requestCounter = Counter.build()
			.name(COUNTER_NAME)
			.help("Total requests for GET /parties/{idType}/{idValue}.")
			.register();

	private static final Histogram requestLatency = Histogram.build()
			.name(HISTOGRAM_NAME)
			.help("Request latency in seconds for GET /parties/{idType}/{idValue}.")
			.register();

	// Prometheus metrics for GET /parties/{idType}/{idValue}/{idSubValue}
	private static final String ROUTE_ID_SUB = "com.modusbox.getPartiesByIdTypeIdValueSubIdValue";
	private static final String COUNTER_NAME_SUB = "counter_get_parties_subId_requests";
	private static final String TIMER_NAME_SUB = "histogram_get_parties_subId_timer";
	private static final String HISTOGRAM_NAME_SUB = "histogram_get_parties_subId_requests_latency";

	public static final Counter requestCounterSub = Counter.build()
			.name(COUNTER_NAME_SUB)
			.help("Total requests for GET /parties/{idType}/{idValue}/{idSubValue}.")
			.register();

	private static final Histogram requestLatencySub = Histogram.build()
			.name(HISTOGRAM_NAME_SUB)
			.help("Request latency in seconds for GET /parties/{idType}/{idValue}/{idSubValue}.")
			.register();

	public void configure() {

		// Add custom global exception handling strategy
		exception.configureExceptionHandling(this);

		from("direct:getPartiesByIdTypeIdValue").routeId(ROUTE_ID).doTry()
				.process(exchange -> {
					requestCounter.inc(1); // increment Prometheus Counter metric
					exchange.setProperty(TIMER_NAME, requestLatency.startTimer()); // initiate Prometheus Histogram metric
				})
				.to("bean:customJsonMessage?method=logJsonMessage(" +
						"'info', " +
						"${header.X-CorrelationId}, " +
						"'Request received at: " + ROUTE_ID + "'," +
						"'fspiop-source: ${header.fspiop-source} Input Payload: ${body}')") // default logging
				/*
				 * BEGIN processing
				 */
				.to("direct:getAuthHeader")
				.setHeader("token", simple("${exchangeProperty.token}"))

//				.process(exchange -> System.out.println())

				.marshal().json()
				.transform((datasonnet("resource:classpath:mappings/postCollectRequest.ds")))
				.setBody(simple("${body.content}"))
//				.process(exchange -> System.out.println())
				.marshal().json(JsonLibrary.Gson)

//				.process(exchange -> System.out.println())

				.removeHeaders("CamelHttp*")
				.removeHeader(Exchange.HTTP_URI)
				.setHeader("Content-Type", constant("application/json"))
				.setHeader("Accept", constant("application/json"))
				.setHeader(Exchange.HTTP_METHOD, constant("POST"))
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
						"'Calling backend API, getParties', " +
						"'Tracking the request', 'Track the response', " +
						"'Request sent to, POST {{dfsp.host}}/okdollar/v1/GetOKUserInfos was: ${body}')")
				.toD("{{dfsp.host}}/okdollar/v1/GetOKUserInfos?bridgeEndpoint=true&throwExceptionOnFailure=false")
//				.unmarshal().json(JsonLibrary.Gson)

//				.marshall().json()
				.unmarshal().json()
//.process(exchange -> System.out.println())
				.choice()
				    .when(simple("${body['code']} != 200"))
				        .to("direct:catchCBSError")
				.endDoTry()
//.process(exchange -> System.out.println())
				.marshal().json()
				.transform(datasonnet("resource:classpath:mappings/getPartiesResponse.ds"))
				.setBody(simple("${body.content}"))
				.marshal().json()

				// Add CORS headers
//				.process(corsFilter)

				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
						"'Response from backend API, getParties: ${body}', " +
						"'Tracking the response', 'Verify the response', null)")
				/*
				 * END processing
				 */
				.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
						"'Send response, " + ROUTE_ID + "', null, null, 'Output Payload: ${body}')") // default logging
				.doCatch(CCCustomException.class, HttpOperationFailedException.class, JSONException.class)
				    .to("direct:extractCustomErrors")
				.doFinally().process(exchange -> {
			((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
		}).end()
		;

	}
}