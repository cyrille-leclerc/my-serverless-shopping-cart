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
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ShoppingCartCheckoutRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    final static Collector<CharSequence, ?, String> INDENTED_LINE_BREAK = Collectors.joining("\n \t", "\t", "");

    private static final Logger logger = LogManager.getLogger(ShoppingCartCheckoutRequestHandler.class);

    private String url = System.getenv("ANTI_FRAUD_URL");
    private OkHttpClient httpClient;

    final static ThreadLocal<List<String>> DOWNSTREAM_HTTP_CALL_HEADERS = new ThreadLocal<>() {
        @Override
        protected List<String> initialValue() {
            return new ArrayList<>();
        }
    };

    public ShoppingCartCheckoutRequestHandler() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor((msg) -> {
            DOWNSTREAM_HTTP_CALL_HEADERS.get().add(msg);

        });
        loggingInterceptor.level(HttpLoggingInterceptor.Level.HEADERS);
        httpClient =
                new OkHttpClient.Builder()
                        .addInterceptor(OkHttpTracing.create(GlobalOpenTelemetry.get()).newInterceptor())
                        .addInterceptor(loggingInterceptor)
                        .build();

        logger.info(() -> "Environment variables: \n" + System.getenv().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).sorted().collect(INDENTED_LINE_BREAK));
        logger.info(() -> "JVM arguments: " + ManagementFactory.getRuntimeMXBean().getInputArguments().stream().sorted().collect(INDENTED_LINE_BREAK));
    }


    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        final SpanContext spanContext = Span.current().getSpanContext();

        logger.info(() ->
                "Checkout: " +
                        "spanContext.spanId=" + spanContext.getSpanId() + ", " +
                        "spanContext.traceId=" + spanContext.getTraceId() + ", " +
                        "spanContext.isRemote=" + spanContext.isRemote() + ", " +
                        "header[traceparent]=" + event.getHeaders().get("traceparent")
        );
        String functionEventHeaders = "Function invocation headers: \n" + event.getHeaders().entrySet().stream().map(entry -> entry.getKey() + ": " + entry.getValue()).sorted().collect(INDENTED_LINE_BREAK);
        logger.info(() -> functionEventHeaders);

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        Request request = new Request.Builder().url(url).build();
        try (Response okhttpResponse = httpClient.newCall(request).execute()) {
            ResponseBody body = okhttpResponse.body();

            String httpResponseBody = body.string();
            String httpInvocationHeaders = DOWNSTREAM_HTTP_CALL_HEADERS.get().stream().collect(INDENTED_LINE_BREAK);

            response.setBody(
                    "Checkout lambda - fetched " + httpResponseBody.length() + " bytes.\n" +
                            "spanContext.spanId=" + spanContext.getSpanId() + ", " +
                            "spanContext.traceId=" + spanContext.getTraceId() + ", " +
                            "spanContext.isRemote=" + spanContext.isRemote() + ", " +
                            "event.header[traceparent]=" + event.getHeaders().get("traceparent") + "\n" +
                            "\n\n" +
                            functionEventHeaders +
                            "\n\n" +
                            "downstreamHttpCallHeaders\n" +
                            httpInvocationHeaders +
                            "\n\n" +
                            "ANTI FRAUD RESPONSE\n" +
                            httpResponseBody

            );
        } catch (IOException e) {
            throw new UncheckedIOException("Could not fetch with okhttp", e);
        } finally {
            DOWNSTREAM_HTTP_CALL_HEADERS.remove();
        }
        return response;
    }
}
