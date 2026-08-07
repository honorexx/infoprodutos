#!/usr/bin/env bash
# Sobe o frontend Next.js em http://localhost:3000 apontando para a API em :8090.
# Usado pela run configuration do IntelliJ e também pode ser chamado manualmente:
#   ./scripts/dev-web.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WEB_DIR="$REPO_ROOT/apps/web"
PORT=3000

cd "$WEB_DIR"

if [ ! -d node_modules ]; then
  echo "Instalando dependências do web (pnpm install)..."
  pnpm install
fi

export NEXT_PUBLIC_API_URL="${NEXT_PUBLIC_API_URL:-http://localhost:8090/api/v1}"

if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Frontend já está rodando em http://localhost:$PORT"
  # Mantém o processo "vivo" para o IntelliJ não marcar a config como terminada.
  while lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; do
    sleep 5
  done
  exit 0
fi

echo "Subindo Next.js em http://localhost:$PORT (API: $NEXT_PUBLIC_API_URL)"
exec pnpm exec next dev --port "$PORT"
