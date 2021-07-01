package com.myecommerce.lambda.antifraud;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.util.stream.Collectors;

public class AntiFraudRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LogManager.getLogger(AntiFraudRequestHandler.class);

    public AntiFraudRequestHandler() {
        logger.info(() -> "Environment variables: " + System.getenv().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(", ")));
        logger.info(() -> "JVM arguments: " + ManagementFactory.getRuntimeMXBean().getInputArguments().stream().collect(Collectors.joining(", ")));
    }

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        Span currentSpan = Span.current();

        final SpanContext spanContext = currentSpan.getSpanContext();
        logger.trace(() -> "AntiFraudRequestHandler: header: [" + event.getHeaders().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).collect(Collectors.joining(",")) + "]");
        logger.info(() ->
                "AntiFraudRequestHandler: " +
                        "spanContext.spanId=" + spanContext.getSpanId() + ", " +
                        "spanContext.traceId=" + spanContext.getTraceId() + ", " +
                        "spanContext.isRemote=" + spanContext.isRemote() + ", " +
                        "header[traceparent]=" + event.getHeaders().get("traceparent")
        );
        OkHttpClient client =
                new OkHttpClient.Builder()
                        .addInterceptor(OkHttpTracing.create(GlobalOpenTelemetry.get()).newInterceptor())
                        .build();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        Request request = new Request.Builder().url("https://www.google.com/").build();
        try (Response okhttpResponse = client.newCall(request).execute()) {
            response.setBody(
                    "Anti Fraud lambda - fetched " + okhttpResponse.body().string().length() + " bytes.\n" +
                            "spanContext.spanId=" + spanContext.getSpanId() + ", " +
                            "spanContext.traceId=" + spanContext.getTraceId() + ", " +
                            "spanContext.isRemote=" + spanContext.isRemote() + ", " +
                            "header[traceparent]=" + event.getHeaders().get("traceparent") + "\n"
                            + "headers:\n" +
                            event.getHeaders().entrySet().stream().map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n").collect(Collectors.joining()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not fetch with okhttp", e);
        }
        return response;
    }
}
