import { createServer } from 'node:http';
import { readFile, writeFile } from 'node:fs/promises';
import { once } from 'node:events';
import { cpus, platform, totalmem } from 'node:os';
import { parseArgs } from 'node:util';

const { values: args } = parseArgs({ options: {
  output: { type: 'string' },
  label: { type: 'string' },
  'ai-url': { type: 'string', default: 'http://127.0.0.1:18088' },
  'ollama-url': { type: 'string', default: 'http://127.0.0.1:11435' },
  'proxy-port': { type: 'string', default: '11436' },
  repetitions: { type: 'string', default: '2' },
  mode: { type: 'string', default: 'both' },
  case: { type: 'string', multiple: true },
} });
const repetitions = Number(args.repetitions);
if (!args.output || !args.label || !process.env.KWATERA_INTERNAL_TOKEN
    || !Number.isSafeInteger(repetitions) || repetitions < 1
    || !['cold', 'warm', 'both'].includes(args.mode)) {
  process.exitCode = 2;
  console.error('Set KWATERA_INTERNAL_TOKEN locally and pass --output FILE --label LABEL [--mode cold|warm|both] [--repetitions N].');
} else {
  await run();
}

async function json(url, body, headers = {}) {
  const response = await fetch(url, {
    method: body === undefined ? 'GET' : 'POST',
    headers: body === undefined ? headers : { 'Content-Type': 'application/json', ...headers },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(250_000),
  });
  return { status: response.status, body: await response.json() };
}

async function run() {
  const corpus = JSON.parse(await readFile(new URL('chat-cases.json', import.meta.url), 'utf8'));
  const messages = args.case ? corpus.filter(sample => args.case.includes(sample.id)) : corpus;
  if (!messages.length || args.case?.some(id => !messages.some(sample => sample.id === id))) {
    throw new Error('Unknown benchmark case ID.');
  }
  const backend = async (path, body) => {
    const result = await json(new URL(path, args['ollama-url']), body);
    if (result.status !== 200) throw new Error(`Ollama ${path} returned HTTP ${result.status}`);
    return result.body;
  };
  let calls = [];
  const proxy = createServer(async (request, response) => {
    try {
      if (!['/api/chat', '/api/tags'].includes(request.url)) {
        response.writeHead(404).end();
        return;
      }
      const buffers = [];
      for await (const buffer of request) buffers.push(buffer);
      const body = request.method === 'POST' ? JSON.parse(Buffer.concat(buffers)) : undefined;
      const result = await json(new URL(request.url, args['ollama-url']), body);
      if (request.url === '/api/chat') {
        const content = result.body.message?.content;
        const detection = 'language' in (body.format?.properties ?? {});
        const source = detection
          ? content?.match(/"language"\s*:\s*"([^"]+)"/)?.[1]
          : body.messages[0].content.match(/^You are a professional .*? \(([a-zA-Z0-9-]+)\) to /)?.[1];
        calls.push({
          model: body.model, detection, source, context: body.options?.num_ctx,
          status: result.status, loadMs: result.body.load_duration / 1e6,
          promptTokens: result.body.prompt_eval_count,
        });
      }
      response.writeHead(result.status, { 'Content-Type': 'application/json' });
      response.end(JSON.stringify(result.body));
    } catch {
      response.writeHead(502, { 'Content-Type': 'application/json' });
      response.end('{"error":"Benchmark proxy request failed"}');
    }
  });
  proxy.listen(Number(args['proxy-port']), '127.0.0.1');
  await once(proxy, 'listening');
  try {
    const { version } = await backend('/api/version');
    const { models } = await backend('/api/tags');
    const health = await json(new URL('/actuator/health/readiness', args['ai-url']));
    if (health.status !== 200) throw new Error(`ai-service is not ready (HTTP ${health.status})`);
    const results = [];
    const unload = async () => {
      for (const model of (await backend('/api/ps')).models) {
        await backend('/api/generate', { model: model.name, keep_alive: 0, stream: false });
      }
      if ((await backend('/api/ps')).models.length) throw new Error('Could not unload all benchmark models');
    };
    const measure = async (sample, phase) => {
      calls = [];
      const start = performance.now();
      const { status, body } = await json(new URL('/internal/ai/translate', args['ai-url']),
        { text: sample.text, targetLanguage: sample.target },
        { 'X-Internal-Token': process.env.KWATERA_INTERNAL_TOKEN });
      const ms = performance.now() - start;
      const translated = status === 200 ? body.translatedText : undefined;
      const fallback = calls.some(call => call.detection);
      const source = calls.findLast(call => call.source)?.source ?? (status === 200 ? sample.target : undefined);
      const row = {
        id: sample.id, phase, ms, status, error: status !== 200 ? body.code : undefined,
        route: fallback ? 'qwen' : status === 200 || calls.length ? 'local' : 'unknown',
        source,
        correctSource: sample.source && source ? sample.source === source.toLowerCase().split('-')[0] : null,
        expectedFallback: sample.fallback ?? false,
        sameLanguageExact: sample.source === sample.target ? translated === sample.text : undefined,
        missingLiterals: sample.preserve?.filter(literal => !translated?.includes(literal)),
        translated, calls,
        residentAfter: (await backend('/api/ps')).models.map(model => ({
          name: model.name, bytes: model.size, vramBytes: model.size_vram,
        })),
      };
      results.push(row);
      if (phase !== 'conditioning') console.log(sample.id, phase, status, ms.toFixed(0) + 'ms', row.route);
    };
    const phases = args.mode === 'both' ? ['cold', 'warm'] : [args.mode];
    for (const phase of phases) {
      await unload();
      if (phase === 'warm') {
        // Prime both model paths if this corpus and configuration exercise them.
        await measure(corpus.find(sample => sample.id === 'en-short'), 'conditioning');
        await measure(corpus[0], 'conditioning');
      }
      for (let repeat = 0; repeat < repetitions; repeat++) {
        for (const sample of messages) {
          if (phase === 'cold') await unload();
          await measure(sample, phase);
        }
      }
    }
    const summary = Object.fromEntries(phases.map(phase => [phase, summarize(results.filter(row => row.phase === phase))]));
    await writeFile(args.output, JSON.stringify({
      label: args.label, version, models: models.map(({ name, digest }) => ({ name, digest })),
      host: { platform: platform(), cpu: cpus()[0]?.model, logicalCpus: cpus().length, ramBytes: totalmem() },
      repetitions, cases: messages.map(sample => sample.id), summary, results,
    }, null, 2) + '\n');
    console.log(JSON.stringify(summary, null, 2));
  } finally {
    proxy.close();
  }
}

function summarize(rows) {
  const successful = rows.filter(row => row.status === 200);
  const latencies = successful.map(row => row.ms).sort((a, b) => a - b);
  const local = rows.filter(row => row.route === 'local').length;
  const qwen = rows.filter(row => row.route === 'qwen').length;
  const calls = rows.flatMap(row => row.calls);
  const loadMs = Object.fromEntries([...new Set(calls.map(call => call.model))].map(model => [
    model,
    median(calls.filter(call => call.model === model && Number.isFinite(call.loadMs))
      .map(call => call.loadMs).sort((a, b) => a - b)),
  ]));
  return {
    successful: successful.length, failed: rows.length - successful.length,
    medianMs: median(latencies),
    p95Ms: latencies[Math.ceil(latencies.length * 0.95) - 1] ?? null,
    local, qwen, localRate: local / rows.length, fallbackRate: qwen / rows.length,
    correctSource: rows.filter(row => row.correctSource === true).length,
    scoredSource: rows.filter(row => row.correctSource !== null).length,
    expectedFallbackMisses: rows.filter(row => row.expectedFallback && row.route !== 'qwen').length,
    literalOrExactMatchFailures: successful.filter(row => row.missingLiterals?.length || row.sameLanguageExact === false).length,
    medianLoadMsByModel: loadMs,
    maxResidentBytes: Math.max(...rows.map(row => row.residentAfter.reduce((sum, model) => sum + model.bytes, 0))),
    maxResidentVramBytes: Math.max(...rows.map(row => row.residentAfter.reduce((sum, model) => sum + model.vramBytes, 0))),
  };
}

function median(sorted) {
  if (!sorted.length) return null;
  return (sorted[Math.floor(sorted.length / 2)] + sorted[Math.floor((sorted.length - 1) / 2)]) / 2;
}
