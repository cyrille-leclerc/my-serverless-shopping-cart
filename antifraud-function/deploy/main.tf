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
    OPENTELEMETRY_COLLECTOR_CONFIG_FILE: "/var/task/opentelemetry-collector.yaml"
    ELASTIC_OTLP_ENDPOINT: var.elastic_otlp_endpoint
    ELASTIC_OTLP_TOKEN: var.elastic_otlp_token
  }

  tracing_mode = var.tracing_mode

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
  enable_xray_tracing = var.tracing_mode == "Active"
}