#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven no está instalado o no está en PATH. Ejecuta el main desde el IDE o instala Maven para usar este script."
  exit 1
fi

mvn -q -DskipTests compile exec:java \
  -Dexec.mainClass=mx.itam.alpha.server.GameServerMain
