# Local + Prod runbook

## Base paths

```bash
export ROOT=/Users/alo/Documents/Code/appCompras/appCompras
export BACKEND=$ROOT/backend
export FRONTEND=$ROOT/frontend
export TF_PROD=$ROOT/infra/envs/prod/runtime
```

## Local dev

```bash
cd $ROOT
./scripts/dev-up.sh app
```

```bash
cd $FRONTEND
npm install
npm run dev
```

```bash
cd $BACKEND
./gradlew --no-daemon test
./gradlew --no-daemon test --tests com.appcompras.security.ApiSecurityAuthTest
```

```bash
cd $ROOT
./scripts/dev-down.sh
```

## CORS configuration

CORS (Cross-Origin Resource Sharing) is controlled via the `APP_CORS_ALLOWED_ORIGINS` environment variable, which accepts a comma-separated list of allowed origins. Only origins in this list are permitted to make cross-origin requests to the API.

### Local dev

For local frontend dev (Vite on port 5173), set:

```bash
export APP_CORS_ALLOWED_ORIGINS="http://localhost:5173"
```

### Production

For the deployed frontend at `https://www.acortesdev.xyz/shopping-app/`, set:

```bash
export APP_CORS_ALLOWED_ORIGINS="https://www.acortesdev.xyz"
```

### CORS behavior

- **Allowed origin**: Preflight `OPTIONS` request returns `200 OK` with `Access-Control-Allow-Origin` header.
- **Rejected origin** (e.g., `https://evil.example.com`): Preflight returns `403 Forbidden`.
- **No origin header**: Request is allowed (used by same-origin requests and some clients).

Test a preflight request:

```bash
curl -i -X OPTIONS "https://api.acortesdev.xyz/api/recipes" \
  -H "Origin: https://www.acortesdev.xyz" \
  -H "Access-Control-Request-Method: POST"
```

## Deploy backend to prod (ECS + Terraform)

### 1) Variables

```bash
export ACCOUNT_ID=430241514032
export AWS_REGION=us-east-1
```

### 2) Set `TAG` (recommended)

Use a unique tag per deploy.

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras
export TAG="$(date +%Y%m%d-%H%M)-$(git rev-parse --short HEAD)"
echo "$TAG"
```

Show latest local commit:

```bash
git log -1 --oneline
```

Show latest remote commit:

```bash
git fetch origin
git log -1 --oneline origin/master
```

### 3) Build and push image

```bash
cd $BACKEND
docker build --platform linux/arm64 -t appcompras-backend-prod:$TAG .

aws ecr get-login-password --region $AWS_REGION \
| docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

docker tag appcompras-backend-prod:$TAG \
${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/appcompras-backend-prod:$TAG

docker push ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/appcompras-backend-prod:$TAG
```

### 4) Update and apply Terraform

```bash
cd $TF_PROD
sed -i '' "s/^backend_image_tag = \".*\"/backend_image_tag = \"$TAG\"/" terraform.tfvars

terraform init
terraform plan
terraform apply
```

## Verify backend in prod

```bash
aws ecs describe-services \
  --cluster appcompras-prod-cluster \
  --services appcompras-prod-backend-svc \
  --query 'services[0].deployments[].{status:status,taskDef:taskDefinition,running:runningCount,pending:pendingCount,rollout:rolloutState}' \
  --output table
```

```bash
aws elbv2 describe-target-health \
  --target-group-arn arn:aws:elasticloadbalancing:us-east-1:430241514032:targetgroup/appcompras-prod-tg/42601481b0e3d08d \
  --query 'TargetHealthDescriptions[].{id:Target.Id,state:TargetHealth.State,reason:TargetHealth.Reason,description:TargetHealth.Description}' \
  --output table
```

```bash
curl -i https://api.acortesdev.xyz/api/recipes
```

```bash
curl -i -X OPTIONS "https://api.acortesdev.xyz/api/shopping-lists/generate?planId=test" \
  -H "Origin: https://www.acortesdev.xyz" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: content-type,authorization,x-api-version,idempotency-key"
```

## Deploy frontend to prod

```bash
cd $FRONTEND
npm install
npm run build
```

Publish `dist/` via your frontend pipeline.
If using S3 + CloudFront:

```bash
aws s3 sync dist/ s3://<YOUR_BUCKET>/shopping-app/ --delete
aws cloudfront create-invalidation --distribution-id <YOUR_DISTRIBUTION_ID> --paths "/shopping-app/*"
```

## Token for Postman

1. Log in at `https://acortesdev.xyz/shopping-app/`
2. In the browser console:

```js
localStorage.getItem('appcompras_id_token')
```

3. Use it in requests:

```http
Authorization: Bearer <TOKEN>
X-API-Version: 1
```

## Placeholders to replace

1. `s3://<YOUR_BUCKET>/shopping-app/` -> actual frontend bucket.
2. `<YOUR_DISTRIBUTION_ID>` -> actual CloudFront distribution.
3. `<TOKEN>` -> current Google ID token.
4. `planId=test` -> real plan UUID for real requests (for preflight `test` is fine).
