package com.myecommerce.lambda.antifraud;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.okhttp.v3_0.OkHttpTracing;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;

public class AntiFraudRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LogManager.getLogger(AntiFraudRequestHandler.class);

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        logger.info("Anti fraud check");

        OkHttpClient client =
                new OkHttpClient.Builder()
                        .addInterceptor(OkHttpTracing.create(GlobalOpenTelemetry.get()).newInterceptor())
                        .build();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        Request request = new Request.Builder().url("https://www.google.com/").build();
        try (Response okhttpResponse = client.newCall(request).execute()) {
            response.setBody(
                    "Anti fraud lambda - fetched " + okhttpResponse.body().string().length() + " bytes.");
        } catch (IOException e) {
            throw new UncheckedIOException("Could not fetch with okhttp", e);
        }
        return response;
    }
}
