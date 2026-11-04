#!/bin/sh
set -eu

# Bound both readiness diagnostics and first-time provisioning. GNU timeout is in
# the pinned Ubuntu-based Ollama image. Failed pulls resume on the next explicit run.
if timeout 15 ollama show "$OLLAMA_MODEL" >/dev/null 2>&1; then
  echo "Configured model already available."
else
  timeout 900 ollama pull "$OLLAMA_MODEL"
fi
