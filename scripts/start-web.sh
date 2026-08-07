#!/usr/bin/env bash
# Atalho local: Postgres (se preciso) não é iniciado aqui — use start-local-postgres.sh.
# Este script sobe só o web; a API fica a cargo do IntelliJ Run / mvn.
set -euo pipefail
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/dev-web.sh"
