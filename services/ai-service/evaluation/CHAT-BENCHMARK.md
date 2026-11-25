# Reservation chat translation benchmark

The benchmark sends synthetic auto-source requests to the real `POST /internal/ai/translate`
endpoint. The reservation client uses this endpoint and also omits the source language.
Timings include HTTP, validation, local detection, inference, and response serialization.
They exclude the gateway, reservation authorization and database lookup, and the browser.
Reservation integration tests cover those service-side checks separately.

## Reproduce locally

Use JDK 25, Maven, Docker, and Node.js 18+. Run the benchmark against a dedicated Ollama
instance. The runner unloads models during cold trials. Use only synthetic messages and a
throwaway internal token defined in the benchmark and AI service shells.

Start a local-only Ollama instance. The pull commands populate its benchmark model volume.

```sh
docker run --rm -d --name kwatera-chat-bench -p 127.0.0.1:11435:11434 -e OLLAMA_KEEP_ALIVE=5m -v kwatera-chat-bench-models:/root/.ollama ollama/ollama:0.40.14
docker exec kwatera-chat-bench ollama pull qwen3:4b-instruct-2507-q4_K_M
docker exec kwatera-chat-bench ollama pull translategemma:4b
mvn -pl services/ai-service -DskipTests package
```

Generate one throwaway value for `KWATERA_INTERNAL_TOKEN` locally and export that value in
both shells. Bash uses `export KWATERA_INTERNAL_TOKEN` after setting the variable.
PowerShell uses `$env:KWATERA_INTERNAL_TOKEN` to set the process environment. Keep
the following `ai-service` process running in the first shell.

```sh
java -jar services/ai-service/target/ai-service-0.0.1-SNAPSHOT.jar --spring.cloud.config.enabled=false --eureka.client.enabled=false --server.address=127.0.0.1 --server.port=18088 --kwatera.ai.base-url=http://127.0.0.1:11436
```

Start the benchmark from the second shell.

```sh
node services/ai-service/evaluation/benchmark-chat.mjs --label optimized-cpu-8192-5m --repetitions 2 --output services/ai-service/target/chat-optimized-cpu.json
```

For each comparison, keep the system, repetitions, and corpus the same.

| Comparison | How to run |
| --- | --- |
| Before the change | Build the same JAR from revision `72ca0b1f1fff9663dbef66cfa3de7e4d0c7d3588` in a separate checkout and launch it with the same flags. Run the benchmark script from this checkout. On the new code, setting `--kwatera.ai.language-detection-enabled=false` also isolates the cost of the detection change. |
| Context | Start the optimized JAR with `--kwatera.ai.translation-context-tokens=2048` or `4096`. The default is 8192. The output records the actual `num_ctx` for each call, including any larger window selected for long prompts. |
| Residency | Stop the dedicated Ollama container and run it again with `-e OLLAMA_KEEP_ALIVE=0` or a finite alternative such as `30s`. Reuse the benchmark volume. With `0`, the warm sequence still runs but models can be reloaded on each call. |
| NVIDIA | Add `--gpus all` to the dedicated Docker run command on a configured machine. Check `ollama ps` or the report's VRAM fields after inference. An accessible GPU can still share the load with the CPU. |

Use a distinct `--label` and `--output` for every run. The script defaults to both phases.
`--mode cold` or `--mode warm` runs one phase. Repeat `--case ID` for a smaller sweep and
use the same selection in every arm of that comparison. Finally stop the temporary Ollama
container with `docker stop kwatera-chat-bench` and exit the local JVM with Ctrl+C.

## Read the results

Cold trials unload resident models before each request. OS page caches and the JVM may
remain warm. Warm trials prime both paths and keep the configured residency policy.
Requests are sequential. The benchmark proxy listens on loopback, forwards Ollama chat
and readiness requests, and extracts the selected tag, model, context, and load time
from the same requests. It records synthetic outputs for language review. A successful
response with neither Ollama call is the local same-language shortcut.

Median and nearest-rank p95 use successful requests. The language correctness count checks
the primary language on samples with one expected language. Compare counts and timeouts before
interpreting speed. Check per-case language tags, expected fallbacks, meaning, tone,
paragraphs, and immutable booking details against [the corpus](chat-cases.json). Literal
checks flag changes such as 22:30 becoming 10:30 PM. Review the flagged values in context.
Compare output text and meaning before accepting a smaller context; long inference can
reach the output token budget even with sufficient input capacity.

`load_duration` includes Ollama's request-side model preparation. `/api/ps` measures model
allocation and GPU allocation after a request. The script reports the maximum sampled
allocation. Peak process RAM and peak GPU usage require separate monitoring.

## Local detection calibration

Lingua 1.2.2 ran against the corpus on Java 25 with the Spring Boot-managed Kotlin
dependencies. The shared detector lazily loads low-accuracy trigram models. It scores
all supported Lingua languages and accepts a local winner for the calibrated set
(English, Polish, German, French, Ukrainian, and Japanese). Qwen handles the other
cases and the translation language support remains governed by the existing models.

Lingua's top score is relative and generally equals 1.0. For the initial corpus, the
English arrival message had a top-two gap of 0.185, and the Polish parking message had
0.456. The English/Spanish mixed message had a gap of 0.104, and the Spanish booking
message with a URL had 0.066 after removing the URL for detection. The 0.15 message
threshold keeps these uncertain cases on Qwen. The mixed English/Ukrainian sample
had a full-message gap of 0.196, and the Ukrainian clause disagreed with a gap of
0.267. A 0.12 fragment threshold screens reliably contradictory clauses and halves of
longer unpunctuated clauses. The fast path requires at least 18 letters and, for
languages other than Japanese, at least four words. This is a small calibration corpus. Short
code switches, unsupported languages with similar spellings, regional variants, and
proper-name-heavy text can still be difficult to classify. The flag provides a rollback
to Qwen for all auto-source TranslateGemma requests.

The pilot let Lingua handle the 448-character Polish booking message. TranslateGemma added
“my colleague” when given the primary tag `pl`. The same message translated without that
addition with Qwen's `pl-PL` tag. The final fast path is limited to 256 UTF-16 units to
keep longer messages on the prior detection path. This limit applies to local detection.
The API continues accepting messages up to its configured input limits.

## Measured results

Measured on 25 November 2026 on Windows 11 with Docker Desktop's Linux engine
(Docker 29.0.1), Intel Core i5-11400H (6 cores, 12 threads), 32 GiB host RAM,
and approximately 15.4 GiB available to Docker. The machine has an NVIDIA GeForce
RTX 3050 Laptop GPU (4 GiB, driver 610.91). Both JVMs used Oracle JDK 25.0.2.
Ollama 0.40.14 used TranslateGemma digest `c49d986b0764` and Qwen digest `0edcdef34593`.
The benchmark runner used Node.js 24.11.1.

### CPU run at the default settings

Both arms used 16 messages per phase, context 8192, and five-minute residency.
The baseline came from the unmodified revision above. The cold median improved
in the final arm. The warm median rose in the full run, while the warm p95 fell.
At this sample size p95 is the maximum and is sensitive to a single slow request.

| Arm | Cold median / p95 | Warm median / p95 | Succeeded | Warm local / Qwen |
| --- | ---: | ---: | ---: | ---: |
| Baseline | 18.21 / 38.17 s | 6.08 / 65.43 s | 32/32 | 0/16 |
| Final | 14.67 / 37.08 s | 9.47 / 36.68 s | 32/32 | 7/9 |

Both arms scored 12/12 correct primary languages in each phase. The final arm sent
all ambiguous, mixed, long, and uncalibrated cases in the corpus to Qwen. Fifteen
of 16 outputs matched the baseline in both phases. The Polish same-language output
improved by preserving the original text and line breaks. The long booking output
preserved the booking references and rendered 22:30 as 10:30 PM in both arms.
Both arms misinterpreted the German check-in question as asking to reserve seats.

The cold median model preparation times for Qwen and TranslateGemma were 4.57 s
and 6.77 s in the baseline, and 4.82 s and 7.31 s in the final arm. Warm medians
were around 1–2 ms for preparation in both arms. Both sampled up to 6.47 GiB in
resident model allocations and zero VRAM usage.

The seven-case NVIDIA subset was also repeated on CPU in two consecutive warm
runs against the same Ollama instance, with the final arm first. Both arms
completed 7/7 requests. Median and p95 were 6.92 s and 27.95 s for the baseline,
and 6.43 s and 33.40 s for the final arm. The full run and this smaller recheck
give different results for the warm CPU median.

One process snapshot after both JVMs had handled translations showed approximately
265 MiB working set for the baseline and 416 MiB for the optimized JVM. This snapshot
includes the JVM, JIT, classpath, and HTTP server alongside Lingua. The incremental
footprint is approximate because the JVMs were sampled at different points in their runs.

### NVIDIA run at the default settings

Both arms used the same seven cases (`en-arrival`, `pl-parking`, `ja-luggage`, `pl-same`,
`en-short`, `mixed-clauses`, and `pl-long`), one repetition per phase, context 8192,
and five-minute residency. The first baseline request included CUDA warm-up and took
139.13 s. Cold results are sensitive to one-time device initialization. With seven
samples the nearest-rank p95 is the maximum, which also makes the warm tail sensitive
to the long booking case.

| Arm | Cold median / p95 | Warm median / p95 | Succeeded | Warm local / Qwen |
| --- | ---: | ---: | ---: | ---: |
| Baseline | 13.61 / 139.13 s | 13.19 / 22.15 s | 14/14 | 0/7 |
| Final | 9.20 / 22.82 s | 1.87 / 28.86 s | 14/14 | 4/3 |

Both arms selected the correct primary language for the six scored cases in each
phase. In both phases the final outputs matched the baseline for six cases, including
the long booking message. The Polish same-language case returned the original with
punctuation, line breaks, and wording preserved. The literal checker also flagged a
clock-format change from 22:30 to 10:30 PM in both arms. It represents the same time.

Both arms sampled allocations up to 2.16 GiB for resident models and 1.50 GiB on GPU.
The baseline warm median model preparation time was 4.47 s for Qwen and 6.36 s
for TranslateGemma. Models were often reloaded while alternating between them on
this GPU even at five-minute residency. The final warm median covers a mix of fast
local routes and slower fallback routes. The warm p95 rose on this sample.

### Context and model residency

The following warm-sequence runs used the same seven-case NVIDIA subset and the
final application. Each setting produced seven successful responses, with the same
text as the final 8192/5m run.

| Preferred translation window | Ollama keep-alive | Median | p95 |
| ---: | ---: | ---: | ---: |
| 8192 | 5m | 1.87 s | 28.86 s |
| 4096 | 5m | 2.51 s | 21.97 s |
| 2048 | 5m | 1.82 s | 25.20 s |
| 8192 | 0 | 8.72 s | 23.34 s |

With preferred context 2048, the long booking case used 8192. With 4096 it used
4096. These small samples support retaining the existing default of 8192. The
`0`-residency run reported no resident models after any measured request, and
TranslateGemma's median preparation time rose from 2.96 s to 5.62 s. The default
residency remains five minutes.
