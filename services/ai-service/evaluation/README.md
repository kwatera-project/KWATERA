# Opt-in translation evaluation

This is separate from Maven and CI. Python 3 (standard library), Docker and the existing
`kwatera-ai-service` (curl transport) and `kwatera-ollama` containers are required. No host
ports are opened. All inputs are synthetic; results contain no tokens, customer data or logs.
Provision the models first, then run from the repository root:

```sh
docker compose --env-file infra/compose/.env -f infra/compose/docker-compose.yml run --rm ollama-init
python services/ai-service/evaluation/evaluate.py --output services/ai-service/evaluation/local-results.json
```

By default all three arms run: `qwen`, `gemma` (selected official prompt), and `gemma-booking`
(rejected general guidance variant). Use `--mode qwen`, `--mode gemma` or `--mode gemma-booking`
to run one arm; `--mode both` runs only the primary model comparison. Each arm unloads resident models before
its first case (do not run alongside other inference). Calls are sequential. Parameters are
temperature 0.1, seed 246, top_k 40, top_p 0.9, context 8192, output budget 1024.
The Qwen arm reproduces the previous service's JSON prompt/schema; Gemma uses the
[official Ollama single-user prompt](https://ollama.com/library/translategemma:4b), including
two blank lines before the payload, without system messages or JSON output constraints.
The [Google model card](https://huggingface.co/google/translategemma-4b-it) requires explicit
source and target codes; its structured Hugging Face processor input is not Ollama's chat API.
The guidance variant adds booking context and data-preservation instructions inside the same
single-user structure. Its regressions and the selection rationale are recorded in [REPORT.md](REPORT.md).
The card describes a 2K training context. The 8192 runtime context here matches the existing
service; short evaluation messages do not establish long-input quality.

`cases.json` contains 30 messages, language directions and review focuses, with selected
immutable literals and paragraph counts. These checks are alerts, **not a quality score**:
e.g. a decimal point becoming a Polish comma can be semantically correct. Name transliteration,
date/number localization, paraphrases and politeness need linguistic review. No exact reference
translation, BLEU score or self-grading by either candidate is used.

Review every output separately for meaning/tone/terminology and naturalness (0 unacceptable,
1 needs correction, 2 acceptable), omitted/changed data, formatting and unsolicited commentary.
Embedded instructions must be translated as message content. Record reasons and uncertainty;
review of languages other than EN/PL by a non-native reviewer is provisional. A model-assisted
assessment is not an independent human/native-speaker acceptance test.

JSON records include full installed digests, model template and parameters, Ollama version,
CPU/VM resources, output, stop reason, token counts, first-call/load time and warm median/max.
`resident_memory` is Ollama `/api/ps` reported model allocation, **not process RSS or peak RAM**;
`size_vram=0` identifies this CPU run. Wall time includes the Docker exec/curl round trip for the
actual request; metadata collection happens outside the timer. Do not infer concurrent-load SLOs from
this single short-message run. Keep reviewed, concise results; do not commit model weights or
raw server/console logs. Newly generated `local-results.json` is ignored.
