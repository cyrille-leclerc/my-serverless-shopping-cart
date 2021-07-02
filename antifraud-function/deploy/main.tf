provider "aws" {
  region  = var.aws_region
}

module "antifraud-lambda-function" {
  source = "terraform-aws-modules/lambda/aws"

  function_name = var.name
  handler       = "com.myecommerce.lambda.antifraud.AntiFraudRequestHandler::handleRequest"
  runtime       = "java11"

  create_package         = false
  local_existing_package = "${path.module}/../target/antifraud-function-1.0-SNAPSHOT.jar"

  memory_size = 384
  timeout     = 20

  layers = compact([
    var.sdk_layer_arn
  ])

  environment_variables = {
    AWS_LAMBDA_EXEC_WRAPPER = "/opt/otel-proxy-handler"
    ELASTIC_OTLP_ENDPOINT: var.elastic_otlp_endpoint
    ELASTIC_OTLP_TOKEN: var.elastic_otlp_token
    OPENTELEMETRY_COLLECTOR_CONFIG_FILE: "/var/task/opentelemetry-collector.yaml"
    OTEL_PROPAGATORS: "tracecontext, baggage" // override the default value "xray, tracecontext,b3, b3multi,"
  }

  tracing_mode = "PassThrough" // ensure xray doesn't modify the trace context. See "api-gateway" enable_xray_tracing below

  attach_policy_statements = true
  policy_statements = {
    s3 = {
      effect = "Allow"
      actions = [
        "s3:ListAllMyBuckets"
      ]
      resources = [
        "*"
      ]
    }
  }
}

module "api-gateway" {
  source = "../../utils/terraform/api-gateway-proxy"

  name                = var.name
  function_name       = module.antifraud-lambda-function.lambda_function_name
  function_invoke_arn = module.antifraud-lambda-function.lambda_function_invoke_arn
  enable_xray_tracing = false // ensure xray doesn't modify the trace context. See AWS Lambda Function attribute `tracing_mode` above

}