# ─────────────────────────────────────────────────────────────────────────────
# Infrastructure
# Provisions the Kubernetes cluster and installs all platform components.
# Usage: make infra
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: infra kubernetes istio keycloak clean

kubernetes:
	@echo "==> [1/3] Starting the Kubernetes cluster managed by OrbStack..."
	orbctl start k8s
	@echo "==> [2/3] Waiting until the Kubernetes API server is reachable and nodes are registered..."
	until kubectl get nodes > /dev/null 2>&1 && [ -n "$$(kubectl get nodes --no-headers 2>/dev/null)" ]; do sleep 2; done
	@echo "==> [3/3] Waiting for all registered nodes to reach Ready state..."
	kubectl wait --for=condition=Ready nodes --all --timeout=120s

istio:
	@echo "==> [1/3] Creating istio-system namespace..."
	kubectl create namespace istio-system --dry-run=client -o yaml | kubectl apply -f -
	@echo "==> [2/3] Installing Istio with default profile via IstioOperator..."
	istioctl install -f infra/kubernetes/istio/01-operator.yaml -y
	@echo "==> [3/3] Applying Gateway manifest..."
	kubectl apply -f infra/kubernetes/istio/02-gateway.yaml

keycloak:
	@echo "==> [1/2] Applying Keycloak manifests..."
	kubectl apply -f infra/kubernetes/keycloak
	@echo "==> [2/2] Waiting for Keycloak to be ready..."
	kubectl rollout status deployment/keycloak -n keycloak --timeout=180s

infra: kubernetes istio keycloak

clean:
	@echo "==> Resetting OrbStack Kubernetes cluster..."
	orbctl stop
	orbctl reset -y
