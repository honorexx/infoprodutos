# Infoprodutos — Plataforma de Cursos Online

> Status atual: **Fase 1 concluída** (fundação: monorepo, autenticação, RBAC). Cursos, vídeos, IA, certificados e pagamentos ainda não foram implementados — ver `docs/ROADMAP.md`.

Plataforma própria de cursos online e infoprodutos (conceitualmente um LMS), com painel administrativo, área do professor e área do aluno, incluindo um pipeline de IA para apoiar a criação de exercícios a partir da transcrição das aulas — sempre com revisão humana obrigatória antes da publicação.

## Documentação

Toda a especificação técnica do projeto vive em [`/docs`](./docs):

| Documento | Conteúdo |
|---|---|
| [`docs/PRD.md`](./docs/PRD.md) | Visão de produto, personas, papéis, escopo do MVP |
| [`docs/ARCHITECTURE.md`](./docs/ARCHITECTURE.md) | Arquitetura, stack, diagramas de componentes e deployment |
| [`docs/DATABASE.md`](./docs/DATABASE.md) | Modelo de dados completo e diagrama entidade-relacionamento |
| [`docs/API.md`](./docs/API.md) | Convenções e mapa de endpoints da API |
| [`docs/AI_PIPELINE.md`](./docs/AI_PIPELINE.md) | Pipeline de IA (transcrição, geração de questões, revisão humana) |
| [`docs/SECURITY.md`](./docs/SECURITY.md) | Modelo de ameaças e mitigações |
| [`docs/ROADMAP.md`](./docs/ROADMAP.md) | Fases de entrega e critérios de aceite (Fase 1 marcada como concluída) |
| [`docs/TEST_STRATEGY.md`](./docs/TEST_STRATEGY.md) | Estratégia de testes |
| [`docs/DESIGN_SYSTEM.md`](./docs/DESIGN_SYSTEM.md) | Sistema visual: paleta, tipografia, componentes, animações e regras de UI |
| [`docs/DECISIONS.md`](./docs/DECISIONS.md) | Registro de decisões, suposições e perguntas abertas (inclui decisões reais da Fase 1) |

## Estrutura do monorepo

```
/apps
  /web     # Next.js (App Router) — painel admin, área do professor, área do aluno
  /api     # Spring Boot — domínio, autenticação, subsistema de IA (fases futuras)
/docs      # Documentação técnica
/infra     # Reservado para infraestrutura como código (fases futuras)
docker-compose.yml   # Postgres + api + web para desenvolvimento local
```

## Stack implementada na Fase 1

- **Frontend:** Next.js 16 (App Router), TypeScript, Tailwind CSS 4, shadcn/ui, React Hook Form + Zod, pnpm.
- **Backend:** Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, Flyway, springdoc-openapi, JWT (jjwt).
- **Banco:** PostgreSQL 16.
- **Testes:** JUnit 5 + Mockito + Testcontainers (backend); Vitest + Testing Library (frontend).

Detalhes e justificativas de cada escolha (incluindo desvios do plano original, como a versão do Next.js): `docs/ARCHITECTURE.md` e `docs/DECISIONS.md`.

## Como rodar localmente

### Opção A — Docker Compose (recomendado)

```bash
cp .env.example .env
# edite .env e defina JWT_SECRET (ex.: openssl rand -base64 48)
# opcionalmente defina DEV_SEED_ADMIN_PASSWORD / DEV_SEED_INSTRUCTOR_PASSWORD / DEV_SEED_STUDENT_PASSWORD
# para ter usuários de teste prontos (SUPER_ADMIN / INSTRUCTOR / STUDENT)

docker compose up --build
```

- API: http://localhost:8080/api/v1 (docs: http://localhost:8080/api-docs/swagger-ui.html)
- Web: http://localhost:3000

> **Nota:** o `docker-compose.yml` e os `Dockerfile`s foram escritos e revisados, mas não puderam ser efetivamente construídos/testados no ambiente usado durante a implementação da Fase 1 (sandbox sem Docker disponível). Rode `docker compose up --build` em um ambiente com Docker antes de considerar este fluxo validado — ver `docs/DECISIONS.md`.

### Opção B — Backend e frontend separados (sem Docker)

Suba um PostgreSQL local dedicado ao projeto (porta 5544, isolado de outros bancos que você já tenha rodando na 5432):

```bash
./scripts/start-local-postgres.sh
```

Backend — no terminal:

```bash
cd apps/api
cp .env.example .env   # preencha DB_* (127.0.0.1:5544 se usou o script acima) e gere um JWT_SECRET
export $(grep -v '^#' .env | xargs)
mvn spring-boot:run
```

Backend — no IntelliJ: já existe uma run configuration pronta, **"ApiApplication (dev)"**, com todas as variáveis de ambiente preenchidas (apontando para o Postgres do script acima). Se o IntelliJ ainda não tiver importado `apps/api/pom.xml` como projeto Maven, ele deve perguntar automaticamente ao abrir o projeto ("Load Maven Project"/"Import Changes") — aceite, e a run configuration passa a funcionar direto.

Frontend:

```bash
cd apps/web
cp .env.example .env.local   # ajuste NEXT_PUBLIC_API_URL se a API não estiver em :8080
pnpm install
pnpm dev
```

## Testes

```bash
# Backend — testes unitários (não requer Docker)
cd apps/api && mvn test

# Backend — testes de integração com Testcontainers (requer Docker)
cd apps/api && mvn verify

# Frontend
cd apps/web && pnpm test
```

## Como contribuir com este momento do projeto

1. Leia `docs/PRD.md` e `docs/ARCHITECTURE.md` para entender o contexto geral.
2. Revise `docs/DECISIONS.md` — perguntas abertas relevantes para as próximas fases (cursos, vídeos, IA, certificados) ainda aguardam confirmação.
3. Nenhuma fase além da Fase 1 (`docs/ROADMAP.md`) deve ser iniciada sem instrução explícita.
