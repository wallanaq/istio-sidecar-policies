# Testando mTLS e AuthorizationPolicy no bin-checker-service

Este guia demonstra dois cenários de bloqueio aplicados ao `bin-checker-service`:

1. **Pod sem mTLS** — bloqueado pelo `PeerAuthentication: STRICT`
2. **Pod com mTLS fora da policy** — bloqueado pela `AuthorizationPolicy`

## Pré-requisitos

- Cluster rodando com Istio instalado (`make infra`)
- Serviços deployados (`make deploy-bin-checker-service deploy-tokenization-service`)
- `kubectl` configurado para o cluster

---

## Obtendo um token de acesso

Ambos os cenários precisam de um JWT válido para exercitar a camada de autorização. Obtenha um token via Keycloak antes de prosseguir:

```bash
TOKEN=$(kubectl run get-token \
  --image=curlimages/curl:latest \
  --namespace=default \
  --rm -it --restart=Never \
  -- curl -s -X POST \
     "http://keycloak-svc.keycloak.svc.cluster.local/realms/master/protocol/openid-connect/token" \
     -d "grant_type=client_credentials" \
     -d "client_id=rest-client" \
     -d "client_secret=mdcmC4K9Jk9ocP9LYIcNSU7NrMEa9Fwq" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

echo $TOKEN
```

---

## Cenário 1 — Pod sem mTLS

### O que está sendo testado

Um pod em namespace **sem** `istio-injection` não recebe sidecar Envoy, portanto
toda conexão de saída é plaintext. O `PeerAuthentication: STRICT` configurado no
namespace `bin-checker-service` faz o sidecar rejeitar qualquer conexão que não
apresente certificado mTLS — a rejeição ocorre no handshake TLS, antes de qualquer
avaliação HTTP.

### Passos

**1. Criar o namespace de teste sem injeção de sidecar:**

```bash
kubectl create namespace test-no-mtls
```

Confirme que o label de injeção não está presente:

```bash
kubectl get namespace test-no-mtls --show-labels
```

**2. Executar a chamada ao bin-checker-service:**

```bash
kubectl run curl \
  --image=curlimages/curl:latest \
  --namespace=test-no-mtls \
  --rm -it --restart=Never \
  -- curl -sv \
     -H "Authorization: Bearer $TOKEN" \
     http://bin-checker-service-svc.bin-checker-service.svc.cluster.local/v1/bin/411111
```

### Resultado esperado

A conexão é encerrada durante o handshake TLS. Não há HTTP response code — o
Envoy fecha o socket antes de processar qualquer requisição HTTP:

```
* Connected to bin-checker-service-svc.bin-checker-service.svc.cluster.local
* Recv failure: Connection reset by peer
* Closing connection
curl: (56) Recv failure: Connection reset by peer
```

---

## Cenário 2 — Pod com mTLS fora da AuthorizationPolicy

### O que está sendo testado

Um pod em namespace **com** `istio-injection` possui sidecar e participa do mTLS.
O handshake TLS ocorre com sucesso e a identidade SPIFFE do pod é verificada. Porém,
o `principal` do certificado — `cluster.local/ns/test-with-mtls/sa/default` — não
consta na `AuthorizationPolicy` do `bin-checker-service`, que permite apenas
`istio-ingressgateway-service-account` e `tokenization-service-sa`. O Envoy rejeita a requisição
na camada RBAC e retorna `403`.

### Passos

**1. Criar o namespace de teste com injeção de sidecar:**

```bash
kubectl create namespace test-with-mtls
kubectl label namespace test-with-mtls istio-injection=enabled
```

Confirme o label:

```bash
kubectl get namespace test-with-mtls --show-labels
```

**2. Executar a chamada ao bin-checker-service:**

```bash
kubectl run curl \
  --image=curlimages/curl:latest \
  --namespace=test-with-mtls \
  --rm -it --restart=Never \
  -- curl -sv \
     -H "Authorization: Bearer $TOKEN" \
     http://bin-checker-service-svc.bin-checker-service.svc.cluster.local/v1/bin/411111
```

### Resultado esperado

O handshake mTLS é concluído, mas o Envoy nega a requisição após avaliar a policy:

```
< HTTP/1.1 403 Forbidden
< content-length: 19
< content-type: text/plain
<
RBAC: access denied
```

---

## Comparando os dois bloqueios

| | Cenário 1 — sem mTLS | Cenário 2 — mTLS fora da policy |
|---|---|---|
| Namespace | `istio-injection` ausente | `istio-injection: enabled` |
| Sidecar | Não | Sim |
| Handshake TLS | Falha | Sucesso |
| Camada de bloqueio | `PeerAuthentication` | `AuthorizationPolicy` |
| Resposta | TCP reset (sem HTTP) | `HTTP 403 RBAC: access denied` |
| Log no Envoy | `RESET` | `denied` |

---

## Inspecionando os logs do Envoy (opcional)

Para confirmar o motivo do bloqueio nos logs do sidecar do `bin-checker-service`:

```bash
# Identifica o pod
POD=$(kubectl get pod -n bin-checker-service -l app=bin-checker-service -o jsonpath='{.items[0].metadata.name}')

# Acompanha os logs do proxy Envoy em tempo real
kubectl logs -n bin-checker-service $POD -c istio-proxy -f
```

- Cenário 1: procure por entradas com `UF` (upstream connection failure) ou `RESET`.
- Cenário 2: procure por `rbac_access_denied_matched_policy` ou `denied`.

---

## Limpeza

```bash
kubectl delete namespace test-no-mtls test-with-mtls
```
