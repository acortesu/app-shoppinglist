terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket = "acortesdev-terraform-state"
    key    = "appcompras/prod/runtime.tfstate"
    region = "us-east-1"
    # DynamoDB lock lo agregamos después (fase siguiente)
  }
}

provider "aws" {
  region = var.aws_region
}