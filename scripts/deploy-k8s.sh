#!/usr/bin/env bash
# Despliega todo en Kubernetes (minikube, kind o Docker Desktop).
#   ./scripts/deploy-k8s.sh
set -euo pipefail
cd "$(dirname "$0")/.."

# 1) Imágenes. En minikube se construyen dentro de su Docker para no usar registry.
if command -v minikube >/dev/null 2>&1 && minikube status >/dev/null 2>&1; then
  eval "$(minikube docker-env)"
fi
docker build -t auth-service:1.0.0 ./auth-service
docker build -t task-service:1.0.0 ./task-service
docker build -t frontend:1.0.0 ./frontend
# En kind: kind load docker-image auth-service:1.0.0 task-service:1.0.0 frontend:1.0.0

# 2) Llaves RSA -> Secret
./scripts/generate-keys.sh
kubectl apply -f k8s/00-namespace.yaml
kubectl create secret generic jwt-keys -n microservicios \
  --from-file=private.pem=keys/private.pem \
  --from-file=public.pem=keys/public.pem \
  --dry-run=client -o yaml | kubectl apply -f -

# 3) Recursos
kubectl apply -f k8s/01-secrets.yaml
kubectl apply -f k8s/02-postgres.yaml
kubectl rollout status deployment/postgres -n microservicios --timeout=120s
kubectl apply -f k8s/03-auth-service.yaml -f k8s/04-task-service.yaml
kubectl rollout status deployment/auth-service -n microservicios --timeout=180s
kubectl rollout status deployment/task-service -n microservicios --timeout=180s
kubectl apply -f k8s/05-frontend.yaml
kubectl rollout status deployment/frontend -n microservicios --timeout=120s

echo
echo "Listo. Página web: NodePort 30080 | auth-service: 30081 | task-service: 30082"
echo "minikube: minikube service frontend -n microservicios"
echo "Alternativa (cualquier clúster), en otra terminal y dejándola abierta:"
echo "   kubectl port-forward -n microservicios svc/frontend 8080:80"
echo "y abre http://localhost:8080"
