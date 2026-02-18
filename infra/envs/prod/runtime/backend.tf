terraform {
  backend "s3" {
    bucket         = "acortesdev-tfstate-prod"
    key            = "appcompras/prod/runtime/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "acortesdev-tf-locks-prod"
    encrypt        = true
  }
}