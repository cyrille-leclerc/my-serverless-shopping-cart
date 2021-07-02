

# Getting started

* Implement your Lambda function extending `com.amazonaws.services.lambda.runtime.RequestHandler`

````java
public class ShoppingCartCheckoutRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        // add your code ...
    }
}
    
````

* Instrument the frameworks you want to get visibility on
   * Example to instrument [OkHttpClient](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/) with OpenTelemetry instrument [io.opentelemetry.instrumentation:opentelemetry-okhttp-3.0:1.3.1-alpha](https://search.maven.org/artifact/io.opentelemetry.instrumentation/opentelemetry-okhttp-3.0/1.3.1-alpha/jar)
````java
import io.opentelemetry.instrumentation.okhttp.v3_0.OkHttpTracing;

OkHttpClient httpClient = new OkHttpClient.Builder()
        .addInterceptor(OkHttpTracing.create(GlobalOpenTelemetry.get()).newInterceptor())
        .build();
````

* Add in the root directory of your lambda function (e.g. `src/main/resources/opentelemetry-collector.yaml`) the configuration of the [AWS Distro for OpenTelemetry Collector](https://github.com/aws-observability/aws-otel-collector) to export the data to Elastic Observability
````yaml
# Copy collector.yaml in the root directory of the lambda function
# Set an environment variable 'OPENTELEMETRY_COLLECTOR_CONFIG_FILE' to '/var/task/opentelemetry-collector.yaml'
receivers:
  otlp:
    protocols:
      http:
      grpc:

exporters:
  logging:
    loglevel: debug
  otlp:
    # Elastic APM server https endpoint without the "https://" prefix
    endpoint: "${ELASTIC_OTLP_ENDPOINT}"
    headers:
      # APM Server secret token
      Authorization: "Bearer ${ELASTIC_OTLP_TOKEN}"

service:
  pipelines:
    traces:
      receivers: [otlp]
      exporters: [logging, otlp]
    metrics:
      receivers: [otlp]
      exporters: [logging, otlp]
````

* Configure you AWS Lambda function with:
   * [Function layer](https://docs.aws.amazon.com/lambda/latest/dg/API_Layer.html): The latest [AWS Lambda layer for OpenTelemetry](https://aws-otel.github.io/docs/getting-started/lambda/lambda-java)  (e.g. `arn:aws:lambda:eu-west-1:901920570463:layer:aws-otel-java-wrapper-ver-1-2-0:1`)
   * [TracingConfig / Mode](https://docs.aws.amazon.com/lambda/latest/dg/API_TracingConfig.html) set to `PassTrough`
   * Export the environment variables:
      * `AWS_LAMBDA_EXEC_WRAPPER="/opt/otel-proxy-handler"` for wrapping handlers proxied through the API Gateway (see [here](https://aws-otel.github.io/docs/getting-started/lambda/lambda-java#enable-auto-instrumentation-for-your-lambda-function)) 
      * `OTEL_PROPAGATORS="tracecontext, baggage"` to override the default setting that also enables X-Ray headers causing interferences between OpenTelemetry and X-Ray
      * `OPENTELEMETRY_COLLECTOR_CONFIG_FILE="/var/task/opentelemetry-collector.yaml"` to specify the path to your OpenTelemetry Collector configuration

* Deploy your Lambda function, test it and visualize it in Elastic Observability's APM view:
* Verify in Elastic Observability / APM
    * Example distributed trace chaining 2 lambda functions 
<img width="250px" src="https://raw.githubusercontent.com/cyrille-leclerc/my-serverless-shopping-cart/main/docs/images/elastic-observability-apm-trace-aws-lambda-java-functions.png" />
  * Example service map chaining 2 lambda functions
<img width="250px" src="https://raw.githubusercontent.com/cyrille-leclerc/my-serverless-shopping-cart/main/docs/images/elastic-observability-apm-service-map-aws-lambda-java-functions.png" />



# Learn more

https://docs.aws.amazon.com/lambda/latest/dg/java-package.html