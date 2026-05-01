aws_region = "us-east-1"

project = "appcompras"
env     = "aws-reference"

tags = {
  Owner = "Alonso"
  Repo  = "appCompras"
}

vpc_cidr             = "10.30.0.0/16"
public_subnet_cidrs  = ["10.30.1.0/24", "10.30.2.0/24"]

container_port   = 8080
desired_count    = 1
cpu              = 256
memory           = 512
healthcheck_path = "/actuator/health"

api_domain      = "api-ref.acortesdev.xyz"
api_domain_name = "api-ref.acortesdev.xyz"

enable_https = true

app_security_require_auth = true

cors_allowed_origins = [
  "https://acortesdev.xyz",
  "https://www.acortesdev.xyz"
]

db_name = "appcompras"