package com.myecommerce.lambda.checkout;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.instrumentation.okhttp.v3_0.OkHttpTracing;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.util.stream.Collectors;

public class ShoppingCartCheckoutRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LogManager.getLogger(ShoppingCartCheckoutRequestHandler.class);

    private String url = System.getenv("ANTI_FRAUD_URL");
    private OkHttpClient httpClient;

    public ShoppingCartCheckoutRequestHandler() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor((msg) -> {
            logger.debug(msg);
        });
        loggingInterceptor.level(HttpLoggingInterceptor.Level.HEADERS);
        httpClient =
                new OkHttpClient.Builder()
                        .addInterceptor(OkHttpTracing.create(GlobalOpenTelemetry.get()).newInterceptor())
                        .addInterceptor(loggingInterceptor)
                        .build();

        logger.info(() -> "Environment variables: " + System.getenv().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(", ")));
        logger.info(() -> "JVM arguments: " + ManagementFactory.getRuntimeMXBean().getInputArguments().stream().collect(Collectors.joining(", ")));
    }


    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        final SpanContext spanContext = Span.current().getSpanContext();

        logger.trace(() -> "Checkout: header: [" + event.getHeaders().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(",")) + "]");
        logger.info(() ->
                "Checkout: " +
                        "span.id=" + spanContext.getSpanId() + ", " +
                        "trace.id=" + spanContext.getTraceId() + ", " +
                        "span.isRemote=" + spanContext.isRemote() + ", " +
                        "header[traceparent]=" + event.getHeaders().get("traceparent")
        );

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        Request request = new Request.Builder().url(url).build();
        try (Response okhttpResponse = httpClient.newCall(request).execute()) {
            response.setBody(
                    "Checkout lambda - fetched " + okhttpResponse.body().string().length() + " bytes.");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not fetch with okhttp", e);
        }
        return response;
    }
}
