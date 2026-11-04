#!/bin/sh
set -eu

# Bound both readiness diagnostics and first-time provisioning. GNU timeout is in
# the pinned Ubuntu-based Ollama image. Failed pulls resume on the next explicit run.
provision() {
  if timeout 15 ollama show "$1" >/dev/null 2>&1; then
    echo "Configured model $1 already available."
  else
    timeout 900 ollama pull "$1"
  fi
}

provision "$OLLAMA_SUMMARY_MODEL"
if [ "$OLLAMA_TRANSLATION_MODEL" != "$OLLAMA_SUMMARY_MODEL" ]; then
  provision "$OLLAMA_TRANSLATION_MODEL"
fi
