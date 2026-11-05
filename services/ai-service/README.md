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
configure its HTTP transport and disable retries and startup model pulling. Operations share one transport/model bean with per-request model selection; a separate
short-timeout client checks availability of both configured models without inference.

Compose pins [Ollama 0.40.14](https://github.com/ollama/ollama/releases/tag/v0.40.14).
Translation defaults to [`translategemma:4b`](https://ollama.com/library/translategemma:4b)
(Q4_K_M, approximately 3.3 GB on disk). Summaries and source-language detection independently
use `qwen3:4b-instruct-2507-q4_K_M` (approximately 2.5 GB on disk). Changing the translator does
not change the summary model. The legacy `OLLAMA_MODEL` variable remains a fallback for the
summary model only; use the explicit variables below when migrating an existing `.env`.

The [30-message comparison and linguistic review](evaluation/REPORT.md) selected TranslateGemma
for better meaning/terminology/naturalness and acceptable CPU cost. The installed full digests are:

- TranslateGemma: `c49d986b0764f5881c476eb21435bb62b7abc62347aab3d4a6071e811be510a1`
- Qwen: `0edcdef34593eac1aa2be9c7d06c432dcf81945adca5eca2f27662c18f168ba0`

Tags can move: compare `ollama list` / `/api/tags` against these evaluated digests after pulling.
Provisioning does not automatically refresh or replace an installed model. No weights are tracked.
The measured CPU allocation was approximately 2.84 GiB for TranslateGemma and 3.62 GiB for Qwen
at context 8192; these are Ollama allocations, not peak process RAM. Budget additional RAM for both
resident models, the runtime and other services. No GPU, paid provider or API key is required.

Translations remain fallible: the selected run confused a security deposit with an advance payment,
changed a quoted token and localized dates/numbers despite preserving their values. A syntactically
valid BCP 47 tag does not prove language support. The assessment is model-assisted, not native-speaker
acceptance. Review consequential messages against their originals; full limitations are in the report.

## Local Compose startup

Prerequisites: Docker Engine/Desktop running, Compose v2, enough disk/RAM, and internet for the
initial image/model downloads. Run these commands from the repository root (POSIX shell):

```sh
# Only if .env does not exist yet:
cp infra/compose/.env.example infra/compose/.env
# Set KWATERA_INTERNAL_TOKEN in .env to a private random value of at least 32 characters.
# Keep it outside version control. Other existing .env values apply to their respective services.
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml up -d --build config-server service-registry ollama
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml run --rm ollama-init
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml up -d --build ai-service
```

In PowerShell use `Copy-Item infra/compose/.env.example infra/compose/.env` for the first command.
The provisioning step checks and provisions both explicit model tags, skipping a duplicate tag.
It uses `ollama show` first and pulls only when missing. The named
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
Other paths, including aggregate health and info, require `X-Internal-Token`, except GET requests
for the OpenAPI/Swagger documentation resources described below.
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
| `OLLAMA_SUMMARY_MODEL` | `qwen3:4b-instruct-2507-q4_K_M` | Summary and source-detection model; falls back to legacy `OLLAMA_MODEL` |
| `OLLAMA_TRANSLATION_MODEL` | `translategemma:4b` | Independent translation model |
| `AI_TRANSLATION_FORMAT` | `TRANSLATE_GEMMA` | `TRANSLATE_GEMMA` single-user text prompt, or `JSON` for instruction models |
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
| `AI_MAX_RESPONSE_CHARACTERS` | `16000` | Maximum accepted model output length |

Both operations use seed 246, top_k 40 and top_p 0.9, matching the evaluation; temperature is
configurable. `AI_REQUEST_TIMEOUT` bounds each HTTP inference call. TranslateGemma auto mode
makes two sequential calls (detection then translation), so allow up to twice that timeout in a
caller, plus transport overhead; explicit source tags avoid detection. There are no automatic retries.

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

### OpenAPI and Swagger UI

The service uses the same `springdoc-openapi-starter-webmvc-ui` dependency and parent-managed
version as `review-service`. With standalone AI listening on port 8088:

- Swagger UI: [http://localhost:8088/swagger-ui.html](http://localhost:8088/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8088/v3/api-docs](http://localhost:8088/v3/api-docs)
- OpenAPI YAML: [http://localhost:8088/v3/api-docs.yaml](http://localhost:8088/v3/api-docs.yaml)

The specification includes both internal operations, request/response DTOs, language and detection
semantics, configurable limits, model selection, and the shared error body/statuses. The `internalToken`
API-key security scheme requires the `X-Internal-Token` header for **both POST operations**. Use
Swagger UI's **Authorize** button only in a trusted local/backend diagnostic environment; never
embed the token in frontend code or examples. Documentation GETs and UI assets load without a token;
other authorization rules are unchanged. Do not put tokens in URLs or share an authorized browser.

Compose keeps AI on the internal network, with no host port or public gateway route. Inspect its
specification from the existing AI container without exposing a port:

```sh
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml exec ai-service sh -c 'curl -fsS "http://localhost:$AI_SERVICE_PORT/v3/api-docs"'
```

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
{"translatedText":"Cześć, o której godzinie mogę się zameldować?","sourceLanguage":"en","targetLanguage":"pl","model":"translategemma:4b"}
```

`text` and `targetLanguage` are required. Languages use syntactically valid BCP 47 tags, for
example `ja`, `ar`, `pt-BR` or `zh-Hant-TW`; there is no Polish/English allowlist. Undefined/private-only
tags and `auto` targets are rejected. Omit `sourceLanguage`, use null, or set it to `auto` to request
language detection. With `TRANSLATE_GEMMA`, the summary model first returns a strictly validated
BCP 47 tag in JSON; an unusable/undetermined result is a controlled 502. That concrete tag is sent
to the translator. Detection is probabilistic, especially on short/mixed-language messages. With
`JSON`, the instruction model detects and translates in one call. In both modes the response reports `sourceLanguage: "auto"`,
describing the requested mode rather than claiming a reliable detected language. Tags are
canonicalized. Exactly matching canonical source/target tags return the original text unchanged
without inference; different regional tags still call the model. The `model` field always reports
the configured translation model, including for this bypass. The intended translation preserves
meaning, tone and message data; the evaluation records where the models fail to do so.

Summaries, detection and `JSON` translation use an Ollama JSON schema and application validation
(single nonempty string field; no extra fields). TranslateGemma follows the
[official Ollama format](https://ollama.com/library/translategemma:4b): one user message with source/
target names and codes and two blank lines before the original text. It has no system message or
JSON output constraint. This differs from the structured processor input described by the
[Google model card](https://huggingface.co/google/translategemma-4b-it).
The service validates plain text for nonempty/size/control-character/truncation errors and wraps
it in the existing JSON DTO. It does not strip comments, replace words or repair terminology.
Protocol validation cannot establish semantic correctness or detect all unsolicited commentary.
The card describes a 2K training context; runtime context 8192 and input limits are retained,
without claiming validated long-input translation quality. Examples illustrate the contract;
wording varies (the measured greeting was “Dzień dobry”, an acceptable tone variant).

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
models, set `OLLAMA_TRANSLATION_MODEL` and its matching `AI_TRANSLATION_FORMAT`, or independently
`OLLAMA_SUMMARY_MODEL`, rerun `ollama-init`, then recreate `ai-service` with `up -d`.
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

## Repeatable live quality and recovery checks

See [evaluation instructions](evaluation/README.md), [cases](evaluation/cases.json) and
[reviewed results](evaluation/REPORT.md). Run outside normal CI, with no concurrent inference:

```sh
python services/ai-service/evaluation/evaluate.py --output services/ai-service/evaluation/local-results.json
python services/ai-service/evaluation/smoke.py --outage --output services/ai-service/evaluation/local-smoke.json
```

The second command calls both secured endpoints, explicit/auto/same-language translations, and
briefly stops **only Ollama**, restoring it in a `finally` block and checking recovery. Run it in a
local development environment. It reads the existing token inside the AI container without printing
or recording it. The recorded [smoke results](evaluation/smoke-results.json) contain only synthetic
requests/results and public health/error statuses. Keep `.env` unchanged; rebuild/recreate only AI:

```sh
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml run --rm ollama-init
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml up -d --no-deps --build ai-service
```

To opt back into the previous translator, set `OLLAMA_TRANSLATION_MODEL=qwen3:4b-instruct-2507-q4_K_M`
and `AI_TRANSLATION_FORMAT=JSON` together, provision and recreate AI. Do not just change a model tag.
