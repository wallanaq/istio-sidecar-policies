package tokenization_service

default allow := false

# Health endpoints are public
allow if {
	health_path
}

# /v1/* requires a valid JWT with the tokenizer.write scope
allow if {
	startswith(input.attributes.request.http.path, "/v1/")
	"tokenizer.write" in token_scopes
}

health_path if {
	path := input.attributes.request.http.path
	path in {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"}
}

# Splits the standard space-separated scope string into a set of individual scopes.
# JWT signature validation is delegated to Istio's RequestAuthentication.
token_scopes := scopes if {
	[_, payload, _] := io.jwt.decode(bearer_token)
	scopes := split(payload.scope, " ")
}

bearer_token := token if {
	auth := input.attributes.request.http.headers.authorization
	startswith(auth, "Bearer ")
	token := substring(auth, 7, -1)
}
