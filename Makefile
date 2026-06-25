# ─────────────────────────────────────────────────────────────────────────────
# Infrastructure
# Provisions the Kubernetes cluster and installs all platform components.
# Usage: make infra
# ─────────────────────────────────────────────────────────────────────────────
ISTIO_VERSION := $(shell istioctl version 2>/dev/null | awk '/client version/{print $$NF; exit}')
ISTIO_ADDONS  := https://raw.githubusercontent.com/istio/istio/$(ISTIO_VERSION)/samples/addons

.PHONY: infra kubernetes istio keycloak observability minio opa clean

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
	@echo "==> [1/4] Ensuring keycloak namespace exists..."
	kubectl create namespace keycloak --dry-run=client -o yaml | kubectl apply -f -
	@echo "==> [2/4] Creating realm ConfigMap from infra/keycloak/realm-export.json..."
	kubectl create configmap keycloak-realm \
		--from-file=realm-export.json=infra/keycloak/realm-export.json \
		-n keycloak --dry-run=client -o yaml | kubectl apply -f -
	@echo "==> [3/4] Applying Keycloak manifests..."
	kubectl apply -f infra/kubernetes/keycloak
	@echo "==> [4/4] Waiting for Keycloak to be ready..."
	kubectl rollout status deployment/keycloak -n keycloak --timeout=180s
	@echo ""
	@echo "    Console → http://keycloak.k8s.orb.local (admin / admin)"

observability:
	@echo "==> Detected Istio version: $(ISTIO_VERSION)"
	@echo "==> [1/3] Installing Prometheus, Grafana, Jaeger and Kiali addons..."
	kubectl apply -f $(ISTIO_ADDONS)/prometheus.yaml
	kubectl apply -f $(ISTIO_ADDONS)/grafana.yaml
	kubectl apply -f $(ISTIO_ADDONS)/jaeger.yaml
	kubectl apply -f $(ISTIO_ADDONS)/kiali.yaml
	@echo "==> [2/3] Waiting for observability stack to be ready..."
	kubectl rollout status deployment/prometheus -n istio-system --timeout=180s
	kubectl rollout status deployment/grafana    -n istio-system --timeout=180s
	kubectl rollout status deployment/jaeger     -n istio-system --timeout=180s
	kubectl rollout status deployment/kiali      -n istio-system --timeout=180s
	@echo "==> [3/3] Exposing dashboards via VirtualServices..."
	kubectl apply -f infra/kubernetes/observability
	@echo ""
	@echo "    Kiali      → http://kiali.k8s.orb.local"
	@echo "    Grafana    → http://grafana.k8s.orb.local"
	@echo "    Prometheus → http://prometheus.k8s.orb.local"
	@echo "    Jaeger     → http://jaeger.k8s.orb.local"

minio:
	@echo "==> [1/3] Applying MinIO manifests..."
	kubectl apply -f infra/kubernetes/minio
	@echo "==> [2/3] Waiting for MinIO to be ready..."
	kubectl rollout status deployment/minio -n minio --timeout=120s
	@echo "==> [3/3] Creating opa-bundles bucket..."
	kubectl run minio-setup \
		--image=minio/mc:latest \
		--namespace=minio \
		--rm --attach --restart=Never --command \
		--overrides='{"metadata":{"annotations":{"sidecar.istio.io/inject":"false"}}}' \
		-- sh -c 'mc alias set local http://minio-svc:9000 minioadmin minioadmin && mc mb local/opa-bundles --ignore-existing && mc version enable local/opa-bundles && mc anonymous set download local/opa-bundles'
	@echo ""
	@echo "    Console → http://minio.k8s.orb.local     (minioadmin / minioadmin)"
	@echo "    API     → http://minio-api.k8s.orb.local"


opa:
	@echo "==> [1/2] Applying OPA manifests..."
	kubectl apply -f infra/kubernetes/opa
	@echo "==> [2/2] Waiting for OPA to be ready..."
	kubectl rollout status deployment/opa -n opa --timeout=120s

infra: kubernetes istio keycloak minio opa

clean:
	@echo "==> Resetting OrbStack Kubernetes cluster..."
	orbctl stop
	orbctl reset -y

# ─────────────────────────────────────────────────────────────────────────────
# Applications
# Builds application JARs and container images.
# Usage: make deploy-bin-checker-service | make deploy-tokenization-service
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: bin-checker-service deploy-bin-checker-service tokenization-service deploy-tokenization-service

bin-checker-service:
	@echo "==> [1/1] Building container image local/apps/bin-checker-service:latest..."
	docker build -f apps/bin-checker-service/Dockerfile -t local/apps/bin-checker-service:latest .

deploy-bin-checker-service: bin-checker-service
	@echo "==> [1/3] Applying bin-checker-service manifests..."
	kubectl apply -f apps/bin-checker-service/infra/kubernetes
	@echo "==> [2/3] Waiting for bin-checker-service to be ready..."
	kubectl rollout status deployment/bin-checker-service -n bin-checker-service --timeout=120s
	@echo "==> [3/3] Done — http://bin-checker-service.k8s.orb.local"

tokenization-service:
	@echo "==> [1/1] Building container image local/apps/tokenization-service:latest..."
	docker build -f apps/tokenization-service/Dockerfile -t local/apps/tokenization-service:latest .

deploy-tokenization-service: tokenization-service
	@echo "==> [1/3] Applying tokenization-service manifests..."
	kubectl apply -f apps/tokenization-service/infra/kubernetes
	@echo "==> [2/3] Waiting for tokenization-service to be ready..."
	kubectl rollout status deployment/tokenization-service -n tokenization-service --timeout=120s
	@echo "==> [3/3] Done — http://tokenization-service.k8s.orb.local"

# ─────────────────────────────────────────────────────────────────────────────
# Policies
# Builds and publishes OPA policy bundles to MinIO.
# No local tooling required — uses ephemeral Docker containers.
# Usage: make build-policies | make publish-policies
# ─────────────────────────────────────────────────────────────────────────────
.PHONY: build-policies publish-policies

build-policies:
	@echo "==> [1/2] Running OPA policy tests..."
	docker run --rm \
		-v $(PWD)/apps/tokenization-service/policy:/src:ro \
		openpolicyagent/opa:latest \
		test /src -v
	@echo "==> [2/2] Building OPA bundle from apps/tokenization-service/policy/..."
	docker run --rm \
		-v $(PWD)/apps/tokenization-service/policy:/src:ro \
		-v /tmp:/out \
		openpolicyagent/opa:latest \
		build -b /src -o /out/tokenization-bundle.tar.gz

publish-policies: build-policies
	@echo "==> [1/1] Uploading bundle to MinIO..."
	docker run --rm \
		--entrypoint sh \
		-v /tmp/tokenization-bundle.tar.gz:/bundle.tar.gz:ro \
		minio/mc:latest \
		-c 'mc alias set local http://minio-api.k8s.orb.local minioadmin minioadmin && mc cp /bundle.tar.gz local/opa-bundles/tokenization-service/bundle.tar.gz'
	@echo ""
	@echo "    Bundle publicado → s3://opa-bundles/tokenization-service/bundle.tar.gz"
