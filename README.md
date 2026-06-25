# Istio Sidecar Policies Lab

A hands-on lab exploring **zero-trust authorization** in a Kubernetes service mesh using Istio, OPA, and Keycloak. The goal is to demystify how multiple security layers work together — and how to split responsibilities cleanly between infrastructure and application concerns.

## What this lab demystifies

### Layered authorization without coupling
Most teams either push all authorization logic into the application or rely solely on a gateway. This lab shows a third path: splitting responsibility across layers, each doing what it does best.

- **Istio `RequestAuthentication`** validates JWT signatures and issuer — cryptographic work the mesh does for free, no application code needed.
- **Istio `AuthorizationPolicy` (ALLOW)** enforces JWT presence at the mesh level — a request without a valid token never reaches the service.
- **OPA `AuthorizationPolicy` (CUSTOM)** enforces business rules — scopes (`tokenizer.write`) and realm roles (`maintainer`) extracted from the JWT payload.

The application itself has zero authorization code.

### Zero-trust by default
An ALLOW policy means the default stance is **deny everything**. Only explicitly listed paths and principals are permitted. Health endpoints are exempt without requiring a JWT — everything else needs one.

### mTLS for service-to-service trust
`PeerAuthentication` enforces mutual TLS in STRICT mode across all namespaces. The `bin-checker-service` goes further: it uses a `DENY` policy to reject requests from any principal that is not the `tokenization-service` service account — even inside the cluster.

### Centralized OPA as an ext_authz provider
A single OPA instance in the `opa` namespace serves as an external authorization provider for Istio via the Envoy `ext_authz` gRPC protocol. Policies are bundled and stored in MinIO; OPA polls for updates every 30–60 seconds. Policy lifecycle is owned by each service — `tokenization-service` ships its own `.rego` bundle alongside its Kubernetes manifests.

---

## Architecture

```
                         ┌─────────────────────────────────┐
                         │         Istio Ingress            │
                         └────────────────┬────────────────┘
                                          │
                    ┌─────────────────────▼──────────────────────┐
                    │           tokenization-service              │
                    │                                             │
                    │  RequestAuthentication  (JWT validation)    │
                    │  AuthorizationPolicy ALLOW  (JWT presence)  │
                    │  AuthorizationPolicy CUSTOM (OPA)           │
                    │  PeerAuthentication STRICT  (mTLS)          │
                    └──────────────┬──────────────────────────────┘
                                   │ mTLS (service account SPIFFE)
                    ┌──────────────▼──────────────────────────────┐
                    │           bin-checker-service               │
                    │                                             │
                    │  AuthorizationPolicy DENY  (principals)     │
                    │  AuthorizationPolicy ALLOW (JWT presence)   │
                    │  PeerAuthentication STRICT  (mTLS)          │
                    └─────────────────────────────────────────────┘

         ┌──────────────┐       ┌──────────────┐       ┌──────────────┐
         │   Keycloak   │       │     OPA      │       │    MinIO     │
         │  (identity)  │       │  (ext_authz) │◄──────│  (bundles)  │
         └──────────────┘       └──────────────┘       └──────────────┘
```

### OPA authorization flow for `POST /v1/*`

```
Request → Istio sidecar → OPA gRPC (ext_authz)
                               │
                               ▼
                    token has tokenizer.write scope?
                    token has maintainer realm role?
                               │
                        allow / deny
```

---

## Prerequisites

| Tool | Purpose |
|---|---|
| [OrbStack](https://orbstack.dev) | Local Kubernetes cluster |
| [Docker](https://www.docker.com) | Build application images; run ephemeral OPA/mc containers |
| [istioctl](https://istio.io/latest/docs/setup/getting-started/) | Install and manage Istio |
| [kubectl](https://kubernetes.io/docs/tasks/tools/) | Interact with the cluster |
| GNU Make | Run lab targets |

No other tools need to be installed locally. OPA and MinIO CLI (`mc`) are invoked via ephemeral Docker containers.

---

## Getting started

### 1. Provision infrastructure

```bash
make infra
```

This runs in order: `kubernetes` → `istio` → `keycloak` → `minio` → `opa`.

### 2. Publish OPA policies

```bash
make publish-policies
```

Runs OPA tests, builds the bundle, and uploads it to MinIO. OPA polls for the new bundle within 60 seconds.

### 3. Deploy applications

```bash
make deploy-bin-checker-service
make deploy-tokenization-service
```

### Tear down

```bash
make clean   # stops and resets the OrbStack cluster
```

---

## Makefile reference

| Target | Description |
|---|---|
| `make infra` | Full infrastructure provisioning |
| `make kubernetes` | Start OrbStack k8s and wait for nodes |
| `make istio` | Install Istio and register OPA as ext_authz provider |
| `make keycloak` | Deploy Keycloak and import the `dev` realm |
| `make minio` | Deploy MinIO and create the `opa-bundles` bucket |
| `make opa` | Deploy OPA |
| `make deploy-tokenization-service` | Build image and apply manifests |
| `make deploy-bin-checker-service` | Build image and apply manifests |
| `make build-policies` | Run OPA tests and build bundle |
| `make publish-policies` | Build bundle and upload to MinIO |
| `make clean` | Reset the cluster |

---

## Services & endpoints

| Service | URL |
|---|---|
| tokenization-service | http://tokenization-service.k8s.orb.local |
| bin-checker-service | http://bin-checker-service.k8s.orb.local |
| Keycloak | http://keycloak.k8s.orb.local |
| MinIO Console | http://minio.k8s.orb.local |
| MinIO API | http://minio-api.k8s.orb.local |
| Kiali | http://kiali.k8s.orb.local |
| Grafana | http://grafana.k8s.orb.local |
| Jaeger | http://jaeger.k8s.orb.local |
| Prometheus | http://prometheus.k8s.orb.local |

---

## Lab identity

**Realm:** `dev`  
**Client:** `rest-client` (confidential, `client_credentials` + `password` grant)

| User | Groups | Roles | Password |
|---|---|---|---|
| `john.doe` | Users, Maintainers | `viewer`, `maintainer` | `password` |
| `jane.doe` | Users | `viewer` | `password` |

Obtain a token (password grant):

```bash
curl -s -X POST http://keycloak.k8s.orb.local/realms/dev/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=rest-client" \
  -d "client_secret=ZoxeWS2LdJEtilfoTxLvJtOnhsnGIQQT" \
  -d "username=john.doe" \
  -d "password=John123" \
  -d "scope=openid tokenizer.write"
```

### Authorization matrix for `POST /v1/card/tokenize`

| Principal | Has JWT | `tokenizer.write` | `maintainer` role | Result |
|---|---|---|---|---|
| Anonymous | No | — | — | 403 |
| `jane.doe` | Yes | Yes | No | 403 |
| `john.doe` | Yes | Yes | Yes | 201 |
| `john.doe` | Yes | No | Yes | 403 |

---

## Technology stack

| Component | Technology | Version |
|---|---|---|
| Service mesh | Istio | 1.30.1 |
| External authorization | OPA (opa-envoy-plugin) | `latest-envoy-static` |
| Identity provider | Keycloak | 26.2 |
| Bundle storage | MinIO | `latest` |
| Applications | Spring Boot | 4.1.0 |
| Runtime | Java | 25 |
| Local Kubernetes | OrbStack | — |

---

## Repository layout

```
.
├── Makefile
├── apps/
│   ├── bin-checker-service/
│   │   ├── infra/kubernetes/        # Deployment, Service, AuthorizationPolicy, mTLS
│   │   └── http-client.rest
│   └── tokenization-service/
│       ├── infra/kubernetes/        # Deployment, Service, AuthorizationPolicy, mTLS
│       ├── policy/
│       │   ├── policy.rego          # OPA policy (scope + role enforcement)
│       │   └── .manifest
│       └── http-client.rest
└── infra/
    ├── keycloak/
    │   └── realm-export.json        # Dev realm (clients, scopes, roles, users)
    └── kubernetes/
        ├── istio/                   # IstioOperator (ext_authz registration), Gateway
        ├── keycloak/
        ├── minio/
        ├── observability/
        └── opa/                     # OPA deployment, config (bundle polling, gRPC)
```
