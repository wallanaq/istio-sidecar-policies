package tokenization_service

default allow := false

allow if {
	health_path
}

allow if {
	startswith(input.attributes.request.http.path, "/v1/")
	"tokenizer.write" in token_scopes
	"maintainer" in token_realm_roles
}

health_path if {
	path := input.attributes.request.http.path
	path in {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}
}

# JWT signature validation is delegated to Istio's RequestAuthentication.
token_payload := payload if {
	[_, payload, _] := io.jwt.decode(bearer_token)
}

token_scopes := split(token_payload.scope, " ")

token_realm_roles := token_payload.realm_access.roles

bearer_token := token if {
	auth := input.attributes.request.http.headers.authorization
	startswith(auth, "Bearer ")
	token := substring(auth, 7, -1)
}
