output "alb_dns_name" {
  value = null
}

output "api_base_url" {
  value = "https://${var.api_domain_name}"
}