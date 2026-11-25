import { spawnSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const composeDir = new URL('./', import.meta.url);
const file = name => fileURLToPath(new URL(name, composeDir));
const compose = ['compose', '--env-file', file('.env'), '-f', file('docker-compose.yml')];
const gpuOverride = ['-f', file('docker-compose.gpu.yml')];
const gpuUnavailable = [
  /could not select device driver.*gpu|unknown runtime.*nvidia/i,
  /no compatible gpu|no devices were found|no nvidia gpu/i,
  /nvidia-smi has failed|nvidia-smi.*(?:executable file not found|command not found)/i,
  /nvidia-container-cli: (?:initialization error|requirement error|device error)/i,
  /failed to initialize nvml|unsatisfied condition: cuda|gpu access blocked/i,
];

export function start(args, docker = runDocker, report = console.log) {
  docker([...compose, 'config', '--quiet']);
  // Let Compose resolve .env quoting and shell precedence. It also returns unrelated variables.
  const environment = docker([...compose, 'config', '--environment']).stdout;
  const requested = environment.match(/^OLLAMA_DEVICE=(.*)$/m)?.[1] ?? 'auto';
  if (!['auto', 'cpu', 'gpu'].includes(requested)) {
    throw new Error('OLLAMA_DEVICE must be auto, cpu, or gpu.');
  }

  let gpu = false;
  if (requested !== 'cpu') {
    docker(['info', '--format', '{{.OSType}}']);
    docker([...compose, ...gpuOverride, 'config', '--quiet']);
    const image = docker([...compose, 'config', '--images', 'ollama']).stdout.trim();
    const probe = docker([
      'run', '--rm', '--gpus', 'driver=nvidia,count=all', '--entrypoint', 'nvidia-smi', image, '-L',
    ], { allowFailure: true });
    const output = probe.stderr + probe.stdout;
    gpu = probe.status === 0 && /^GPU \d+:/m.test(probe.stdout);
    if (probe.status !== 0 && !gpuUnavailable.some(pattern => pattern.test(output))) {
      throw new Error(`Ollama GPU probe failed (Docker exit ${probe.status}):\n${output.trim()}`);
    }
    if (!gpu) {
      const detail = output.trim() || 'No NVIDIA device was visible inside Docker.';
      if (requested === 'gpu') {
        throw new Error(`OLLAMA_DEVICE=gpu requires an NVIDIA GPU accessible to Docker.\n${detail}`);
      }
      report(`Ollama NVIDIA GPU unavailable.\n${detail}`);
    }
  }

  report(`Ollama execution mode: ${gpu ? 'NVIDIA GPU' : 'CPU'} (OLLAMA_DEVICE=${requested}).`);
  return docker([...compose, ...(gpu ? gpuOverride : []), 'up', ...args], { inherit: true }).status;
}

function runDocker(args, { allowFailure = false, inherit = false } = {}) {
  const result = spawnSync('docker', args, { encoding: 'utf8', stdio: inherit ? 'inherit' : 'pipe' });
  if (result.error) throw result.error;
  if (!allowFailure && !inherit && result.status !== 0) {
    throw new Error(`docker ${args[0]} failed (exit ${result.status}):\n${result.stderr.trim()}`);
  }
  return { ...result, status: result.status ?? 1 };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    const args = process.argv.slice(2);
    process.exitCode = start(args.length ? args : ['--build']);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
