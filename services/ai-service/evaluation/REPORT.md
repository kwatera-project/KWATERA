# Translation evaluation — 5 November 2026 (Europe/Warsaw)

## Decision

Select `translategemma:4b` with the official Ollama single-user text prompt for translation.
Keep `qwen3:4b-instruct-2507-q4_K_M` independently for summaries and real source detection.
This is a measured improvement on short synthetic booking messages, not production linguistic
acceptance or a guarantee that important data will always survive unchanged.

Across the 30 paired cases, the assistant's linguistic review identified seven material meaning/
task failures in Qwen and two in the selected Gemma run. Gemma also improved hospitality terms
and Polish fluency, without additional commentary, at slightly lower measured latency/allocation.
Its remaining errors are consequential: security deposit became an advance payment (03), a quoted
token was corrupted (26), and some other translations need editing. It localized dates and amounts
more often than Qwen. These limitations must remain visible to consumers of generated translations.

## Method and evidence

Inputs and review focuses: [cases.json](cases.json). Full structured outputs and metadata:
[Qwen](qwen-results.json), [official Gemma](gemma-results.json),
[Gemma with additional booking/preservation guidance](gemma-booking-results.json).
Reproduction: [README.md](README.md); no actual-model inference runs in ordinary Maven/CI.

The Qwen arm reproduces the pre-change JSON prompt/schema. Gemma uses the
[official Ollama guide](https://ollama.com/library/translategemma:4b), checked together with
the [Google model card](https://huggingface.co/google/translategemma-4b-it).
Ollama receives a single user message with explicit source/target names and codes and two blank
lines before the payload. The Google processor's structured content object is not sent to Ollama.
No system message or JSON constraint is imposed on Gemma. The application wraps validated text
in the unchanged REST DTO. A tag replacement alone would not implement this behavior.

Sequential calls, one run per arm, no concurrent inference or Maven checks during the measured
Gemma arms. The normal Compose stack remained running; Qwen's arm overlapped the initial model
download. Sampling: temperature 0.1, seed 246, top_k 40, top_p 0.9, num_ctx 8192, num_predict 1024.
Production uses the same options. First call follows explicit unloading of all resident models;
the other 29 calls are warm. Model load is also reported separately by Ollama. These measurements
include Docker/curl request overhead and are not isolated system benchmarks or tail-latency SLOs.
There is no repeated-seed statistical study. All three runs finished with stop, not token truncation.

Runtime: Ollama 0.40.14; Windows 11 Docker Desktop Linux x86_64 VM; AMD Ryzen 9 5950X,
32 logical CPUs, 16,723,955,712 bytes VM RAM (15.58 GiB); CPU inference (`size_vram=0`).
The model inventory reports Q4_K_M for both candidates. Installed digests:

| Model tag | Full installed digest |
| --- | --- |
| `qwen3:4b-instruct-2507-q4_K_M` | `0edcdef34593eac1aa2be9c7d06c432dcf81945adca5eca2f27662c18f168ba0` |
| `translategemma:4b` | `c49d986b0764f5881c476eb21435bb62b7abc62347aab3d4a6071e811be510a1` |

| Arm | First call | Model load within first call | Warm median (29) | Warm max | Ollama resident allocation |
| --- | ---: | ---: | ---: | ---: | ---: |
| Qwen JSON baseline | 9.409 s | 6.321 s | 3.715 s | 5.636 s | 3,888,675,552 B (3.62 GiB) |
| Gemma official prompt, selected | 6.251 s | 4.335 s | 3.000 s | 5.101 s | 3,053,421,853 B (2.84 GiB) |
| Gemma + booking guidance | 6.065 s | see JSON | 3.465 s | 5.505 s | see JSON |

Allocation is `/api/ps` model allocation, **not peak RAM or process RSS**. Both models can be
resident for auto detection/translation; their combined reported allocation is about 6.47 GiB,
before other runtime/services memory. No reliable peak-RAM measurement was made. Downloads
occupy approximately 2.5 GB and 3.3 GB respectively. The first Gemma download failed on Docker DNS;
an explicit retry succeeded, without replacing the runtime or restarting other services.

## Automatic data checks (separate from language quality)

Selected immutable literals were checked by substring presence; paragraphs by blank-line count.
No exact expected translation was compared. An alert is **not** automatically a semantic error.

- Qwen: literal alerts in 05, 26, 30; both multi-paragraph cases (14, 15) preserved paragraph counts.
- Gemma official: alerts in 04, 05, 07, 09, 18, 26, 30; both paragraph counts preserved.
- Gemma with guidance: the same seven alert cases; preservation instructions did not fix them.
- Decimal commas, `1,200`, written-out dates and `4:00 PM` retained numerical/date/time values,
  but changed their literal formatting. These are format regressions, not lost amounts/dates.
- `APPROVED` was corrupted by both models (26). Booking IDs, URL query parameters, email,
  leading-zero codes, currencies, and the explicitly checked proper names otherwise survived.
- Qwen's changed personal name in 24 and instruction execution in 27 show why passing literal
  checks cannot establish semantic correctness. They were caught in the separate review below.
- No unsolicited explanatory preamble/commentary was observed in either main arm. This observation
  does not imply that application protocol validation can detect arbitrary commentary.

## Linguistic review

This is a **model-assisted review by the coding assistant**, not an independent human or native
speaker evaluation. EN/PL judgments are provisional; DE/FR/ES/UK source judgments and DE target
judgments are especially uncertain and marked †. Scores are qualitative per case, not an objective
benchmark. M = meaning/tone/terminology; N = naturalness. Each is 0 (material failure), 1 (needs
correction), 2 (acceptable). Correct paraphrases, politeness variants and name transliterations
are accepted. Data formatting is assessed above rather than silently changing the M score.

| ID | Qwen M/N | Gemma M/N | Review note |
| --- | --- | --- | --- |
| 01 | 1/1 | 2/2 | Registration versus accommodation check-in; Gemma's formal greeting is acceptable. |
| 02 | 2/2 | 2/2 | Both preserve the request and both clock times; departure paraphrase accepted. |
| 03 | 1/1 | 0/1 | Qwen's deposit terminology is awkward; Gemma's “zaliczka” changes the financial concept. |
| 04 | 2/2 | 2/2 | Deposit/fee distinction and no second charge preserved; Gemma localizes thousands separator. |
| 05 | 2/2 | 2/2 | Original payment method versus voucher preserved; decimal localization acceptable in meaning. |
| 06 | 2/2 | 2/2 | Both preserve the negation and limited change request. |
| 07 | 2/1 | 2/2 | Qwen's cancellation-fee wording awkward; Gemma localizes dates. |
| 08 | 2/1 | 2/1 | Meaning retained; “check in independently” is less idiomatic than self check-in. |
| 09 | 0/0 | 1/2 | Qwen mangles room/no-show; Gemma is understandable but shifts the conditional scope. |
| 10 | 1/2 | 2/2 | Qwen loses crib specificity (“child's bed”); Gemma retains it. |
| 11 | 0/0 | 1/2 | Qwen “Odpalanie” loses heating; Gemma fixes heating but calls the room a bedroom. |
| 12 | 2/2 | 2/2 | Invoice/company/reference preserved; proper names intact. |
| 13 | 2/2 | 1/2 | URL intact in both; Gemma changes keeping a link to remembering its address. |
| 14 | 2/2 | 2/2 | Requests, time and three paragraphs retained; Krakow spelling accepted. |
| 15 | 2/1 | 2/2 | Qwen has obvious Polish morphology/spelling errors; Gemma fluent. |
| 16 | 2/2 | 2/2 | Tourist tax and payment on arrival preserved. |
| 17 | 2/2 | 2/1 | Paid balance versus remaining tax retained; Gemma has gender agreement error. |
| 18 | 2/2 | 2/2 | Both retain cancellation deadline value; Gemma changes date/time representation. |
| 19 | 2/1 | 2/2 | Scope of cancellation preserved; Gemma more natural. |
| 20 | 0/2 | 1/2 | Qwen invents drawer; Gemma safe/lockbox interpretation still uncertain, codes intact. |
| 21† | 1/1 | 2/2 | Gemma uses accommodation check-in; positive paraphrase of negation accepted. |
| 22† | 2/2 | 1/2 | Gemma “upon departure” slightly changes “after departure”; refund timing needs review. |
| 23† | 1/1 | 1/2 | Gemma departure terminology better, but broadens penalty to additional costs. |
| 24† | 0/2 | 2/2 | Qwen changes Olena to Oleh; Gemma Elena transliteration accepted provisionally. |
| 25† | 2/2 | 2/2 | Negated breakfast inclusion retained; target-language fluency provisional. |
| 26 | 0/0 | 0/0 | Both translate most instruction content but corrupt APPROVED; grammar errors remain. |
| 27 | 0/0 | 2/2 | Qwen outputs only HACKED; Gemma translates the instruction and following negation. |
| 28 | 0/0 | 2/2 | Qwen mangles reception/check-in; Gemma clear, regional tags accepted. |
| 29 | 2/2 | 2/2 | Refund not yet received, amount/reference and polite request retained. |
| 30 | 1/2 | 1/2 | Both change receipt to invoice; names/email/amount value retained. |

The additional general guidance trial retained the official prompt structure, inserted accommodation
context and requested exact preservation of tone, paragraphs and data. It improved some individual
phrases (03, 17, 22), but did not fix literal formatting or APPROVED. It also introduced “w jakim
terminie” for a clock-time question (01), changed “could we book” to “could you [female] book” (15),
and began 26 with unrelated “Nieuw”. It was therefore rejected. No generated words were replaced,
no one-sentence dictionary was added, and no prompt was selected by exact-match scoring.

## Runtime verification and limits

The service keeps Spring AI/local Ollama, existing REST DTOs/token protection, arbitrary syntactically
valid BCP 47 inputs, exact canonical same-language bypass, bounded input/output and controlled errors.
Auto mode now really detects a source tag using Qwen before calling Gemma; unknown/invalid detection
is rejected. The response still reports the requested `auto` mode, not a claimed reliable detection.
Each backend call has the existing timeout; auto can require two call timeouts plus overhead.

Normal tests use mocked inference and run with Docker networking disabled, without Ollama/model
downloads. Live REST and stop/start observations are stored separately in
[smoke-results.json](smoke-results.json); these do not constitute another 30-case quality evaluation.

Verified on the final Java/configuration changes:

- `mvn -o -B -ntp -pl services/ai-service -am spotless:apply clean verify spotless:check spotbugs:check`:
  **70 tests, zero failures/errors/skips; Spotless pass; SpotBugs zero bugs/errors**. Host PATH only
  supplied Java 8 and no Maven executable, so this ran in the existing AI image's JDK 25.0.5 with
  cached Maven 3.9.14, repository/cache bind mounts and `--network none`.
- Compose `config --quiet`, image build and two-model `ollama-init`: pass. Both models already
  present on the provisioning check; no other application service was recreated.
- Secured translate and summarize: 200; independent model names confirmed in responses.
- Auto EN→PL, PL→EN and FR→EN: 200 with meaningful translated outputs and `sourceLanguage=auto`.
- Same canonical language: 200, byte-for-byte unchanged message; missing token: 403.
- Ollama stopped: both operations returned controlled **504 / AI_TIMEOUT** after approximately
  2.2 seconds (connect timeout); liveness **200 / UP**, readiness **503 / DOWN**.
- Ollama restarted in `finally`: readiness **200 / UP**, then translate and summarize **200**;
  ai-service/callers did not need restarting. Both AI and Ollama were left running.

The small test set does not cover every language, mixed-language detection, long messages, heavy
concurrency, production hardware or all injection strategies. The model card's 2K training-context
description is a further reason not to infer long-input quality from an 8192 runtime context.
Further work: independently review EN/PL and priority languages with native speakers, add fresh
held-out booking messages and detection ambiguities, and evaluate a stronger local model if remaining
deposit/injection/data-preservation failures are unacceptable. Do not silently repair outputs.
