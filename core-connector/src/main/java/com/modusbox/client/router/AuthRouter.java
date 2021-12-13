package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.CorsFilter;
import com.modusbox.client.processor.EncodeAuthHeader;
import com.modusbox.client.processor.TokenStore;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class AuthRouter extends RouteBuilder {

    private final String PATH_NAME = "OK Dollar Fetch Access Token API";
    private final String PATH = "/okdollar/v1/auth/token";

    private final RouteExceptionHandlingConfigurer exceptionHandlingConfigurer = new RouteExceptionHandlingConfigurer();

    public void configure() {

        exceptionHandlingConfigurer.configureExceptionHandling(this);

        from("direct:getAuthHeader")
                .to("bean:customJsonMessage?method=logJsonMessage(" +
                        "'info', " +
                        "${header.X-CorrelationId}, " +
                        "Request received at: getAuthHeader," +
                        "'fspiop-source: ${header.fspiop-source} Input Payload: ${body}')") // default logging
                .setProperty("downstreamRequestBody", simple("${body}"))
                .removeHeaders("Camel*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setBody(constant(""))
                .marshal().json()

                .setProperty("token", method(TokenStore.class, "getAccessToken()"))

                .choice()
                .when(method(TokenStore.class, "getAccessToken()").isEqualTo(""))

                .transform(datasonnet("resource:classpath:mappings/postAuthTokenRequest.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()
                .to("bean:customJsonMessage?method=logJsonMessage(" +
                        "'info', " +
                        "${header.X-CorrelationId}, " +
                        "'Calling the " + PATH_NAME + "', " +
                        "null, " +
                        "null, " +
                        "'Request to POST {{dfsp.host}}" + PATH + ", IN Payload: ${body}')")
                .toD("{{dfsp.host}}" + PATH)
                .unmarshal().json()

                .to("bean:customJsonMessage?method=logJsonMessage(" +
                        "'info', " +
                        "${header.X-CorrelationId}, " +
                        "'Called " + PATH_NAME + "', " +
                        "null, " +
                        "null, " +
                        "'Response from POST {{dfsp.host}}" + PATH + ", OUT Payload: ${body}')")
//                    .process(exchange -> System.out.println())
                .setProperty("token", simple("${body['data']['token']}"))
                .setProperty("tokenExpiration", simple("${body['data']['ExpiresIn']}"))
//                    .process(exchange -> System.out.println())
                .bean(TokenStore.class, "setAccessToken(${exchangeProperty.token}, ${exchangeProperty.tokenExpiration})")
//                    .process(exchange -> System.out.println())
                .to("bean:customJsonMessage?method=logJsonMessage(" +
                        "'info', " +
                        "${header.X-CorrelationId}, " +
                        "'Auth Token caught from " + PATH_NAME + "', " +
                        "null, " +
                        "null, " +
                        "'token: ${exchangeProperty.token}')")
                .removeHeaders("CamelHttp*")
                .end()
                .setBody(simple("${exchangeProperty.downstreamRequestBody}"))
        ;
    }
}