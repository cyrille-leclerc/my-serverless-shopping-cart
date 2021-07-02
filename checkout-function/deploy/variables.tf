variable "name" {
  type        = string
  description = "Name of created function and API Gateway"
  default     = "checkout"
}

variable "aws_region" {
  type        = string
  description = "AWS Region"
}

variable "sdk_layer_arn" {
  type        = string
  description = "ARN for the Lambda layer containing the OpenTelemetry Java Wrapper & Otel Collector"
  // See https://aws-otel.github.io/docs/getting-started/lambda/lambda-java
  // Example: arn:aws:lambda:eu-west-1:901920570463:layer:aws-otel-java-wrapper-ver-1-2-0:1
}

variable "tracing_mode" {
  type        = string
  description = "Lambda function X-RAY tracing mode ('PassThrough' or 'Active')"
  default     = "PassThrough"
}

variable "elastic_otlp_endpoint" {
  type        = string
  description = "Elastic OTLP endpoint (e.g. 'apm-server.elastic.mycompany.com:443')"
}

variable "elastic_otlp_token" {
  type        = string
  sensitive   = true
  description = "Elastic OTLP token (aka Elastic APM Server Token)"
}

variable "anti_fraud_url" {
  type        = string
  description = "URL of the Anti Fraud endpoint"
}