output "api-gateway-url" {
  value = module.api-gateway.api_gateway_url
}

output "function_role_name" {
  value = module.checkout-lambda-function.lambda_role_name
}