#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TF_DIR="$ROOT/infra/terraform"
NAMESPACE="pulse-tickets"

terraform -chdir="$TF_DIR" init -input=false
terraform -chdir="$TF_DIR" apply -auto-approve -input=false
CLUSTER_NAME="$(terraform -chdir="$TF_DIR" output -raw eks_cluster_name)"
aws eks wait cluster-active --region "$AWS_REGION" --name "$CLUSTER_NAME"
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"

for attempt in {1..30}; do
  kubectl auth can-i get pods --all-namespaces >/dev/null 2>&1 && break
  sleep 10
done
kubectl auth can-i get pods --all-namespaces >/dev/null

SECRET_ARN="$(terraform -chdir="$TF_DIR" output -raw deployment_secret_arn)"
SECRET_JSON="$(aws secretsmanager get-secret-value --secret-id "$SECRET_ARN" --query SecretString --output text)"
DATABASES="$(terraform -chdir="$TF_DIR" output -json database_endpoints)"
export USERS_DB_ENDPOINT="$(jq -r '.users' <<<"$DATABASES")"
export EVENTS_DB_ENDPOINT="$(jq -r '.events' <<<"$DATABASES")"
export TICKETS_DB_ENDPOINT="$(jq -r '.tickets' <<<"$DATABASES")"
export PAYMENTS_DB_ENDPOINT="$(jq -r '.payments' <<<"$DATABASES")"
export KAFKA_BOOTSTRAP_SERVERS="$(terraform -chdir="$TF_DIR" output -raw kafka_bootstrap_servers)"
export BANNER_BUCKET="$(terraform -chdir="$TF_DIR" output -raw banner_bucket)"
export BANNER_CDN_URL="$(terraform -chdir="$TF_DIR" output -raw banner_cdn_url)"
export ECR_REGISTRY="$(terraform -chdir="$TF_DIR" output -json ecr_repository_urls | jq -r '.["api-gateway"]' | cut -d/ -f1)"
export IMAGE_TAG="${IMAGE_TAG:?IMAGE_TAG must be a pushed immutable image tag}"

kubectl apply -f "$ROOT/k8s/namespace.yaml"
kubectl -n "$NAMESPACE" create secret generic platform-secrets \
  --from-literal=JWT_SECRET="$(jq -r '.JWT_SECRET' <<<"$SECRET_JSON")" \
  --from-literal=ADMIN_USERNAME="$(jq -r '.ADMIN_USERNAME' <<<"$SECRET_JSON")" \
  --from-literal=ADMIN_EMAIL="$(jq -r '.ADMIN_EMAIL' <<<"$SECRET_JSON")" \
  --from-literal=ADMIN_PASSWORD="$(jq -r '.ADMIN_PASSWORD' <<<"$SECRET_JSON")" \
  --from-literal=DATABASE_USER="$(jq -r '.DATABASE_USER' <<<"$SECRET_JSON")" \
  --from-literal=DATABASE_PASSWORD_USERS="$(jq -r '.DATABASE_PASSWORD_USERS' <<<"$SECRET_JSON")" \
  --from-literal=DATABASE_PASSWORD_EVENTS="$(jq -r '.DATABASE_PASSWORD_EVENTS' <<<"$SECRET_JSON")" \
  --from-literal=DATABASE_PASSWORD_TICKETS="$(jq -r '.DATABASE_PASSWORD_TICKETS' <<<"$SECRET_JSON")" \
  --from-literal=DATABASE_PASSWORD_PAYMENTS="$(jq -r '.DATABASE_PASSWORD_PAYMENTS' <<<"$SECRET_JSON")" \
  --dry-run=client -o yaml | kubectl apply -f -

envsubst < "$ROOT/k8s/production.yaml" | kubectl apply -f -
kubectl -n "$NAMESPACE" rollout restart deployment/user-service
kubectl -n "$NAMESPACE" rollout status deployment/api-gateway --timeout=10m
kubectl -n "$NAMESPACE" rollout status deployment/user-service --timeout=10m
kubectl -n "$NAMESPACE" rollout status deployment/event-service --timeout=10m
kubectl -n "$NAMESPACE" rollout status deployment/ticket-service --timeout=10m
kubectl -n "$NAMESPACE" rollout status deployment/payment-service --timeout=10m
kubectl -n "$NAMESPACE" rollout status deployment/notification-service --timeout=10m
kubectl -n "$NAMESPACE" rollout status deployment/analytics-service --timeout=10m

for attempt in {1..30}; do
  API_ORIGIN="$(kubectl -n "$NAMESPACE" get service api-gateway -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
  [[ -n "$API_ORIGIN" ]] && break
  sleep 10
done
[[ -n "${API_ORIGIN:-}" ]] || { echo "API gateway load balancer was not provisioned" >&2; exit 1; }

terraform -chdir="$TF_DIR" apply -auto-approve -input=false -var="api_origin_domain=$API_ORIGIN"
WEB_ORIGIN="$(terraform -chdir="$TF_DIR" output -raw frontend_url)"
[[ -n "$WEB_ORIGIN" ]] || { echo "Frontend CloudFront URL was not provisioned" >&2; exit 1; }
envsubst < "$ROOT/k8s/production.yaml" | kubectl apply -f -
kubectl -n "$NAMESPACE" rollout restart deployment/api-gateway
kubectl -n "$NAMESPACE" rollout status deployment/api-gateway --timeout=10m
WEB_BUCKET="${BANNER_BUCKET/-event-banners-/-web-}"
aws s3 sync "$ROOT/web/dist/event-ticketing-web/browser" "s3://$WEB_BUCKET" --delete
DIST_ID="$(terraform -chdir="$TF_DIR" output -raw frontend_distribution_id)"
aws cloudfront create-invalidation --distribution-id "$DIST_ID" --paths '/*' >/dev/null
echo "Deployment complete: $WEB_ORIGIN"
