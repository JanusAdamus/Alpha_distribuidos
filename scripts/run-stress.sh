#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven no está instalado o no está en PATH. Corre el estrés desde la ventana del servidor o instala Maven para usar este script."
  exit 1
fi

if [ "$#" -eq 0 ]; then
  ARGS="--clients=10,50,100 --hits=10 --repetitions=10 --output=samples/generated/stress-results.csv"
else
  ARGS="$*"
fi

mvn -q -DskipTests compile exec:java \
  -Dexec.mainClass=mx.itam.alpha.stress.StressTestMain \
  -Dexec.args="$ARGS"
