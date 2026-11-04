# Shared AI service

Issue [#246](https://github.com/kwatera-project/KWATERA/issues/246). Stateless, independently
deployable Spring Boot service providing review summaries and message translation. No database,
message history, persistence changes or automatic business-workflow integration. Existing features
do not depend on this service or Ollama.

## Runtime and model

Java 25, Spring Boot 4.0.5 and Spring Cloud 2025.1.1 follow the parent reactor.
[Spring AI 2.0.2 supports Boot 4.0.x](https://docs.spring.io/spring-ai/reference/getting-started.html).
The module imports its own stable Spring AI BOM; it does not change auth-service's AI dependency.
The official `spring-ai-ollama` module supplies `OllamaChatModel` and `OllamaApi`. Explicit Spring beans
configure its HTTP transport and disable retries and startup model pulling. Controllers share one
model bean; a separate short-timeout client checks model availability without inference.

Compose pins [Ollama 0.40.14](https://github.com/ollama/ollama/releases/tag/v0.40.14).
The default model is
[`qwen3:4b-instruct-2507-q4_K_M`](https://ollama.com/library/qwen3:4b-instruct-2507-q4_K_M):
a compact instruction model with multilingual and structured JSON support, approximately 2.5 GB on
disk. The instruction-only variant avoids spending the response budget on reasoning traces.
This is a development compromise between CPU cost and quality. Budget roughly 5–6 GB RAM
for Ollama, plus RAM for the Java services; CPU latency depends heavily on hardware and input size.
No GPU, paid provider or API key is required. Larger models may improve translation quality.
Language support and factual accuracy depend on the selected model; accepting a BCP 47 tag does
not prove that the model can translate that language. Validate quality before relying on results.
The default is intended for local development; evaluate representative language pairs and use a
stronger model when production translation quality requires it.
The live English-to-Polish example sometimes translated accommodation check-in as registration.
Smoke tests establish working inference and JSON contracts, not linguistic acceptance. Production
language-pair evaluation, especially hospitality terminology, remains necessary.

## Local Compose startup

Prerequisites: Docker Engine/Desktop running, Compose v2, enough disk/RAM, and internet for the
initial image/model downloads. Run these commands from the repository root (POSIX shell):

```sh
cp infra/compose/.env.example infra/compose/.env
# Set KWATERA_INTERNAL_TOKEN in .env to a private random value of at least 32 characters.
# Keep it outside version control. Other existing .env values apply to their respective services.
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml up -d --build config-server service-registry ollama
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml run --rm ollama-init
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml up -d --build ai-service
```

In PowerShell use `Copy-Item infra/compose/.env.example infra/compose/.env` for the first command.
The provisioning step uses `ollama show` first and pulls only when missing. The named
`ollama-models` volume survives container recreation. Provisioning times out after 15 minutes;
repeat the command to resume a failed download. Startup does not wait for model provisioning.
If provisioning fails, HTTP/liveness still start and inference returns a controlled error.
No existing service gains a dependency on Ollama. Compose's default network supplies DNS names;
neither Ollama nor ai-service publishes a host port.

Diagnostics and a request from inside the AI container:

```sh
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml exec ai-service sh -c 'curl -fsS "http://localhost:$AI_SERVICE_PORT/actuator/health/liveness"'
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml exec ai-service sh -c 'curl -fsS "http://localhost:$AI_SERVICE_PORT/actuator/health/readiness"'
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml exec ai-service sh -c 'curl -fsS -H "X-Internal-Token: $KWATERA_INTERNAL_TOKEN" -H "Content-Type: application/json" "http://localhost:$AI_SERVICE_PORT/internal/ai/translate" -d "{\"text\":\"Hello, what time can I check in?\",\"sourceLanguage\":\"en\",\"targetLanguage\":\"pl\"}"'
```

`/actuator/health/liveness` reflects only application liveness. Readiness includes the Ollama
model inventory and returns 503 if the backend/model is missing; it does not warm up the model or
guarantee inference quality/free memory. Probes disclose only status and require no token.
Other paths, including aggregate health and info, require `X-Internal-Token`.
Compose checks liveness so a temporarily missing model does not cause restart loops.

## Configuration

Environment variables override defaults in `application.yaml` and `config-repo/ai-service.yml`:

| Variable | Default | Purpose |
| --- | --- | --- |
| `KWATERA_INTERNAL_TOKEN` | required, at least 32 characters | Shared private backend token; no built-in secret |
| `AI_SERVICE_PORT` | `8088` | Application port |
| `CONFIG_SERVER_URL` | `http://localhost:8888` | Optional Config Server import; Compose supplies `http://config-server:8888` |
| `EUREKA_DEFAULT_ZONE` | `http://localhost:8761/eureka/` | Compose supplies `http://service-registry:8761/eureka/` |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Compose supplies `http://ollama:11434` |
| `OLLAMA_MODEL` | `qwen3:4b-instruct-2507-q4_K_M` | Explicit model size/instruction/quantization tag |
| `AI_CONNECT_TIMEOUT` | `2s` | HTTP connection timeout |
| `AI_REQUEST_TIMEOUT` | `120s` | Synchronous inference HTTP timeout; increase for slower CPUs |
| `AI_READINESS_TIMEOUT` | `2s` | Inventory probe HTTP timeout |
| `AI_TEMPERATURE` | `0.1` | Generation temperature, range 0–2 |
| `AI_MAX_OUTPUT_TOKENS` | `1024` | Ollama `num_predict`; truncated output is rejected |
| `AI_CONTEXT_TOKENS` | `8192` | Ollama context window |
| `AI_MAX_REVIEWS` | `20` | Reviews per request |
| `AI_MAX_REVIEW_CHARACTERS` | `2000` | Per-review length |
| `AI_MAX_TOTAL_REVIEW_CHARACTERS` | `6000` | Sum of review lengths |
| `AI_MAX_TRANSLATION_CHARACTERS` | `2000` | Message length |
| `AI_MAX_RESPONSE_CHARACTERS` | `16000` | Maximum accepted model JSON length |

Lengths count Java UTF-16 code units. Choose limits appropriate to the model's tokenizer/context
window and output budget, particularly for non-Latin text. The service does not chunk/truncate input
on behalf of callers. Long translations can exceed the output budget and return 502.
For variables not explicitly forwarded by Compose, add them to the `ai-service.environment` block
or supply a Compose override. Config Server reads the `ai-service` configuration and shared
`application.yml`; shared datasource settings do not create a database dependency because this
module has no JDBC/JPA starter. Config Server's optional import has bounded connect/read timeouts.
Eureka registers the application as `ai-service`; business logic contains no container addresses.
For standalone development set `SPRING_CLOUD_CONFIG_ENABLED=false` and `EUREKA_CLIENT_ENABLED=false`
if those infrastructure services are unavailable.

## Internal REST contracts

All requests use `Content-Type: application/json` and `X-Internal-Token`. No gateway route exposes
these endpoints. The token follows existing KWATERA internal-call conventions but has no insecure
default. Configure the same token in trusted backend callers. Use a private network and TLS/mTLS
at the infrastructure boundary outside local development; keep the token out of browser clients.
The service checks authorization before JSON parsing and inference. It does not change existing JWT
authentication or grant end users access.

### POST /internal/ai/summarize

```json
{"reviews":["The apartment was clean and comfortable.","Great location, but Wi-Fi was unstable.","Friendly host and easy check-in."],"language":"en"}
```

```json
{"summary":"Guests appreciate the cleanliness, location and friendly host, but some reported unreliable Wi-Fi.","model":"qwen3:4b-instruct-2507-q4_K_M"}
```

`reviews` must contain one or more non-null, non-blank strings. `language` is required and must
be a BCP 47 language tag. The prompt requests a concise balanced summary based only on supplied
reviews. Review content is serialized as JSON in a separate user message and explicitly treated
as untrusted data. No model tools are registered. Prompts reduce prompt-injection/hallucination
risk but cannot establish semantic correctness; consumers should preserve access to originals.

### POST /internal/ai/translate

```json
{"text":"Hello, what time can I check in?","sourceLanguage":"en","targetLanguage":"pl"}
```

```json
{"translatedText":"Cześć, o której godzinie mogę się zameldować?","sourceLanguage":"en","targetLanguage":"pl","model":"qwen3:4b-instruct-2507-q4_K_M"}
```

`text` and `targetLanguage` are required. Languages use syntactically valid BCP 47 tags, for
example `ja`, `ar`, `pt-BR` or `zh-Hant-TW`; there is no Polish/English allowlist. Undefined/private-only
tags and `auto` targets are rejected. Omit `sourceLanguage`, use null, or set it to `auto` to ask
the model to detect the language. In that case the response reports `sourceLanguage: "auto"`,
describing the requested mode rather than claiming a reliable detected language. Tags are
canonicalized. Exactly matching canonical source/target tags return the original text unchanged
without inference; different regional tags still call the model. The `model` field always reports
the configured model, including for this bypass. Translation prompting requests preservation of
tone, paragraph structure, URLs, references and numbers, with no additional commentary.

Both operations constrain output using an Ollama JSON schema and validate the response again.
Malformed, empty, wrong-type, extra-field and token-truncated output is rejected, never returned
as a successful inference. Example texts above illustrate the contract; model wording varies.

For a standalone JVM listening on localhost with Ollama available there:

```sh
curl -fsS http://localhost:8088/internal/ai/summarize \
  -H "X-Internal-Token: $KWATERA_INTERNAL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"reviews":["Clean room.","Wi-Fi was unstable."],"language":"en"}'
curl -fsS http://localhost:8088/internal/ai/translate \
  -H "X-Internal-Token: $KWATERA_INTERNAL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"text":"Hello, what time can I check in?","targetLanguage":"pl"}'
```

### Errors

```json
{"code":"AI_UNAVAILABLE","message":"The AI backend is temporarily unavailable.","status":503,"timestamp":"2026-11-04T20:00:00Z"}
```

| Code | HTTP | Meaning |
| --- | --- | --- |
| `INVALID_REQUEST` | 400 | Missing/blank fields, empty reviews or malformed JSON |
| `INVALID_LANGUAGE` | 400 | Invalid language identifier |
| `ACCESS_DENIED` | 403 | Missing/wrong internal token |
| `METHOD_NOT_ALLOWED` | 405 | Unsupported HTTP method |
| `INPUT_TOO_LARGE` | 413 | Configured input limit exceeded |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | JSON content type required |
| `INTERNAL_ERROR` | 500 | Unexpected application processing error |
| `AI_INVALID_RESPONSE` | 502 | Invalid/incomplete inference output or backend protocol error |
| `AI_UNAVAILABLE` | 503 | Unreachable, overloaded or failed AI backend |
| `AI_MODEL_NOT_READY` | 503 | Configured model missing (backend 404) |
| `AI_TIMEOUT` | 504 | Inference timeout |

Errors omit stack traces, backend bodies and input contents. No automatic retries run in the
service. Callers should choose short bounded retry/backoff or show an unavailable state while
retaining the original reviews/messages. User workflow success must not require AI availability.

## Calling from another backend

Use the existing Eureka/load-balancer pattern with a `@LoadBalanced RestClient.Builder` (Spring
Cloud LoadBalancer dependency required), resolve `http://ai-service/internal/ai/translate`, and
set `X-Internal-Token` from backend configuration. A request body uses the DTO contract above;
decode the JSON response into the caller's own DTO. Set a bounded caller timeout compatible with
the inference timeout. Do not route through the public gateway or forward tokens supplied by
browser clients. Direct Compose DNS is also available for internal diagnostic requests.
No caller service was changed to persist or automatically request AI output in this issue.

## Tests and quality

With JDK 25 and Maven 3.9+ from the repository root:

```sh
mvn -B -ntp -pl services/ai-service -am clean verify
mvn -B -ntp -pl services/ai-service -am spotless:check spotbugs:check
```

Alternatively, inside `services/ai-service` run `./mvnw clean verify` (`.\mvnw.cmd` on Windows).
The wrapper version follows the other services. CI's Java quality matrix runs these checks,
JaCoCo and SonarQube Cloud without a live model or model download. Tests mock inference, exercise
REST validation/security/errors/diagnostics and start the real configuration without Ollama.
An actual Spring AI model over a mocked official Ollama API verifies a single failed attempt.

The Sonar project key is `kwatera-project_KWATERA_ai-service`. A Sonar organization administrator
must ensure this project exists, is bound to this repository, has the intended quality gate and
allows analysis by the existing `SONAR_TOKEN`; `SONAR_ORGANIZATION` must be configured as for the
other modules. The unauthenticated Sonar API lookup returned 404, "Component key not found";
an administrator must provision the project if absent or verify access if it is private.
Repository changes cannot provision or verify those external permissions. Sonar analysis is
enabled in CI, but successful cloud analysis is not claimed until the CI job passes.

## Troubleshooting and optional live smoke checks

```sh
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml logs ai-service ollama
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml exec ollama ollama list
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml exec ollama ollama show qwen3:4b-instruct-2507-q4_K_M
```

For 503/model missing, rerun the provisioning command. For 504 on CPU, increase inference and caller
timeouts or select a smaller model. For 502, inspect the model's structured-output support and
output-token budget; the service deliberately does not return a fake success. For poor language
quality, choose a stronger multilingual model and evaluate representative examples. To change
models, set `OLLAMA_MODEL` in `.env`, rerun `ollama-init`, then recreate `ai-service` with `up -d`.
No weights are committed and there is no automatic pull on Java startup. Tags pin the selected
variant; upstream registries can still update tags, so production deployments should record and
validate the model digest shown by `ollama list`.

After startup, run both API requests above from the AI container, inspect Eureka at
`http://localhost:8761` for `AI-SERVICE`, then stop Ollama and repeat a model-backed request:

```sh
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml stop ollama
# Model-backed requests return 503/504; liveness stays UP and readiness becomes DOWN.
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml start ollama
# Readiness and requests recover without restarting callers or ai-service.
```

These live smoke checks require a running Docker daemon and downloaded model; they are separate
from deterministic Maven tests. Do not treat mocked tests as evidence of model quality, live
Eureka registration or real CPU inference performance.
