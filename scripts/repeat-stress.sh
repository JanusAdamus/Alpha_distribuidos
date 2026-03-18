#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven no está instalado o no está en PATH. Corre el estrés desde la ventana del servidor o instala Maven para usar este script."
  exit 1
fi

CLIENTS="${1:-10,50,100,150,200,250,300,350,400,450,500}"
HITS="${2:-12}"
REPETITIONS="${3:-10}"
OUTPUT="${4:-samples/generated/stress-results.csv}"

./scripts/run-stress.sh \
  --clients="$CLIENTS" \
  --hits="$HITS" \
  --repetitions="$REPETITIONS" \
  --output="$OUTPUT"
