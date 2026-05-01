project_id = "menu-shopping-list"
region     = "us-east1"

cors_allowed_origins = "https://www.acortesdev.xyz,https://acortesdev.xyz"

# Image tag will be set via CLI: -var="image_tag=20240430-2245-abc123f"
# image_tag = "latest"

# Scaling
min_instances = 0   # Scale to zero when idle
max_instances = 3
