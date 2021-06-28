variable "name" {
  type        = string
  description = "Name of created function and API Gateway"
  default     = "checkout"
}

variable "collector_layer_arn" {
  type        = string
  description = "ARN for the Lambda layer containing the OpenTelemetry collector extension"
  // TODO(anuraaga): Add default when a public layer is published.
  // https://aws-otel.github.io/docs/getting-started/lambda/lambda-java

  // arn:aws:lambda:eu-west-3:901920570463:layer:opentelemetry-lambda-extension:8
}

variable "sdk_layer_arn" {
  type        = string
  description = "ARN for the Lambda layer containing the OpenTelemetry Java Wrapper"
  // TODO(anuraaga): Add default when a public layer is published.
  // arn:aws:lambda:eu-west-1:901920570463:layer:aws-otel-java-wrapper-ver-1-2-0:1
}

variable "tracing_mode" {
  type        = string
  description = "Lambda function tracing mode"
  default     = "PassThrough"
}