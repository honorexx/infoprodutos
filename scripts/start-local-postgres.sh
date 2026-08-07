#!/usr/bin/env bash
# Sobe um PostgreSQL 16 local e isolado apenas para o projeto Infoprodutos,
# em uma porta dedicada (5544) para não conflitar com outros bancos locais
# (ex.: o Postgres padrão em 5432 usado por outros projetos).
#
# Uso: ./scripts/start-local-postgres.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PGDATA="$REPO_ROOT/.localpg"
PORT=5544
PG_BIN="/opt/homebrew/opt/postgresql@16/bin"

if lsof -nP -iTCP:"$PORT" -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Postgres já está rodando na porta $PORT."
  exit 0
fi

if [ ! -d "$PGDATA" ]; then
  echo "Inicializando novo data directory em $PGDATA ..."
  "$PG_BIN/initdb" -D "$PGDATA" -U infoprodutos --auth=trust >/dev/null
fi

echo "Subindo Postgres na porta $PORT (data dir: $PGDATA) ..."
"$PG_BIN/pg_ctl" -D "$PGDATA" -l "$REPO_ROOT/.localpg.log" \
  -o "-p $PORT -k /tmp" start

# Aguarda o banco aceitar conexões antes de garantir database/role.
for _ in $(seq 1 20); do
  if "$PG_BIN/pg_isready" -h 127.0.0.1 -p "$PORT" >/dev/null 2>&1; then
    break
  fi
  sleep 0.5
done

"$PG_BIN/psql" -h 127.0.0.1 -p "$PORT" -U infoprodutos -d postgres -tc \
  "SELECT 1 FROM pg_database WHERE datname = 'infoprodutos'" | grep -q 1 \
  || "$PG_BIN/createdb" -h 127.0.0.1 -p "$PORT" -U infoprodutos infoprodutos

echo "Postgres pronto em 127.0.0.1:$PORT (db=infoprodutos, user=infoprodutos)."
