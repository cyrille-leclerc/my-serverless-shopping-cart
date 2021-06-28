
# Configuration

## `terraform.tfvars`

```hcl

aws_region="eu-west-1"
sdk_layer_arn="arn:aws:lambda:eu-west-1:901920570463:layer:aws-otel-java-wrapper-ver-1-2-0:1"

elastic_otlp_endpoint="***.apm.***.elastic-cloud.com:443"
elastic_otlp_token="***"
```


# Setup

Prerequisite:
* `terraform` [CLI](https://www.terraform.io/downloads.html) 
* AWS credentials, either using environment variables or via the CLI and aws configure


```shell
terraform init

terraform apply
```