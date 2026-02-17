# Runbook Local + Prod

## Paths base

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
gradle --no-daemon test
gradle --no-daemon test --tests com.appcompras.security.ApiSecurityAuthTest
```

```bash
cd $ROOT
./scripts/dev-down.sh
```

## Deploy backend a prod (ECS + Terraform)

### 1) Variables

```bash
export ACCOUNT_ID=430241514032
export AWS_REGION=us-east-1
```

### 2) Definir `TAG` (recomendado)

Usa un tag único por deploy.

```bash
cd /Users/alo/Documents/Code/appCompras/appCompras
export TAG="$(date +%Y%m%d-%H%M)-$(git rev-parse --short HEAD)"
echo "$TAG"
```

Ver último cambio (commit):

```bash
git log -1 --oneline
```

Ver último commit en remoto:

```bash
git fetch origin
git log -1 --oneline origin/master
```

### 3) Build y push de imagen

```bash
cd $BACKEND
docker build --platform linux/arm64 -t appcompras-backend-prod:$TAG .

aws ecr get-login-password --region $AWS_REGION \
| docker login --username AWS --password-stdin ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com

docker tag appcompras-backend-prod:$TAG \
${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/appcompras-backend-prod:$TAG

docker push ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/appcompras-backend-prod:$TAG
```

### 4) Actualizar y aplicar Terraform

```bash
cd $TF_PROD
sed -i '' "s/^backend_image_tag = \".*\"/backend_image_tag = \"$TAG\"/" terraform.tfvars

terraform init
terraform plan
terraform apply
```

## Verificación backend en prod

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

## Deploy frontend prod

```bash
cd $FRONTEND
npm install
npm run build
```

Publicar `dist/` con el mecanismo de frontend que uses.
Si usas S3 + CloudFront:

```bash
aws s3 sync dist/ s3://<TU_BUCKET>/shopping-app/ --delete
aws cloudfront create-invalidation --distribution-id <TU_DISTRIBUTION_ID> --paths "/shopping-app/*"
```

## Token para Postman

1. Login en `https://acortesdev.xyz/shopping-app/`
2. En consola:

```js
localStorage.getItem('appcompras_id_token')
```

3. Usar en requests:

```http
Authorization: Bearer <TOKEN>
X-API-Version: 1
```

## Placeholders que debes reemplazar

1. `s3://<TU_BUCKET>/shopping-app/` -> bucket real frontend.
2. `<TU_DISTRIBUTION_ID>` -> CloudFront distribution real.
3. `<TOKEN>` -> Google ID token vigente.
4. `planId=test` -> UUID de plan para request real (en preflight puede quedarse `test`).
