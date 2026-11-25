import assert from 'node:assert/strict';
import { test } from 'node:test';
import { start } from './start.mjs';

function fixture(mode, probe = { status: 0, stdout: 'GPU 0: NVIDIA', stderr: '' }) {
  const commands = [];
  const messages = [];
  return {
    commands, messages,
    start: (...args) => start(args, (command) => {
      commands.push(command);
      if (command.includes('--environment')) {
        return { stdout: mode === undefined ? '' : `OLLAMA_DEVICE=${mode}\r\n` };
      }
      if (command.includes('--images')) return { stdout: 'ollama/ollama:pinned\n' };
      if (command[0] === 'run') return probe;
      return { status: 0 };
    }, message => messages.push(message)),
  };
}

test('CPU mode skips GPU discovery and forwards Compose up arguments', () => {
  const run = fixture('cpu');
  assert.equal(run.start('-d', 'ollama'), 0);
  assert.deepEqual(run.commands.at(-1).slice(-3), ['up', '-d', 'ollama']);
  assert.ok(run.commands.every(command => command[0] === 'compose'));
  assert.match(run.messages.at(-1), /CPU/);
});

test('auto uses the override after a successful container probe', () => {
  const run = fixture();
  assert.equal(run.start('-d'), 0);
  assert.ok(run.commands.some(command => command[0] === 'run' && command.includes('ollama/ollama:pinned')));
  assert.ok(run.commands.at(-1).some(argument => argument.endsWith('docker-compose.gpu.yml')));
  assert.match(run.messages.at(-1), /NVIDIA GPU/);
});

test('auto falls back for known device errors and an empty GPU list', () => {
  for (const probe of [
    { status: 125, stderr: 'could not select device driver "nvidia" with capabilities: [[gpu]]', stdout: '' },
    { status: 0, stderr: '', stdout: '' },
  ]) {
    const run = fixture('auto', probe);
    assert.equal(run.start(), 0);
    assert.ok(!run.commands.at(-1).some(argument => argument.endsWith('docker-compose.gpu.yml')));
    assert.match(run.messages.at(-1), /CPU/);
  }
});

test('GPU-required fails when Docker cannot expose a device', () => {
  const run = fixture('gpu', { status: 1, stderr: 'Failed to initialize NVML', stdout: '' });
  assert.throws(() => run.start(), /requires an NVIDIA GPU accessible to Docker/);
  assert.ok(run.commands.every(command => !command.includes('up')));
});

test('invalid modes and unrelated probe errors do not start Compose', () => {
  const invalid = fixture('rocm');
  assert.throws(() => invalid.start(), /must be auto, cpu, or gpu/);
  const run = fixture('auto', { status: 125, stderr: 'denied: requested access to the resource', stdout: '' });
  assert.throws(() => run.start(), /Docker exit 125.*\n.*denied/);
  assert.ok(run.commands.every(command => !command.includes('up')));
});
