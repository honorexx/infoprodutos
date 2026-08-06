# Infoprodutos — Plataforma de Cursos Online

> Status atual: **fase de documentação técnica**. Nenhum código de aplicação foi implementado ainda. A implementação só será iniciada após aprovação explícita da arquitetura.

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
| [`docs/ROADMAP.md`](./docs/ROADMAP.md) | Fases de entrega e critérios de aceite |
| [`docs/TEST_STRATEGY.md`](./docs/TEST_STRATEGY.md) | Estratégia de testes |
| [`docs/DECISIONS.md`](./docs/DECISIONS.md) | Registro de decisões, suposições e perguntas abertas |

## Estrutura planejada do monorepo

```
/apps
  /web     # Next.js — painel admin, área do professor, área do aluno
  /api     # Spring Boot — domínio, autenticação, subsistema de IA
/docs      # Documentação técnica (este conjunto de arquivos)
/infra     # docker-compose e configuração de ambiente local
```

`/apps` e `/infra` ainda não existem neste repositório — serão criados a partir da Fase 0 do [`ROADMAP.md`](./docs/ROADMAP.md), após aprovação da arquitetura.

## Stack (proposta, ver `ARCHITECTURE.md` para justificativas)

- **Frontend:** Next.js, TypeScript, Tailwind CSS, shadcn/ui.
- **Backend:** Java 21, Spring Boot 3.3.x, Spring Security, Spring Data JPA, Flyway, springdoc-openapi.
- **Banco:** PostgreSQL 16.
- **Testes:** JUnit 5, Mockito, Testcontainers (backend); Vitest/Playwright (frontend).

## Como contribuir com este momento do projeto

Antes de qualquer implementação de código:
1. Leia `docs/PRD.md` e `docs/ARCHITECTURE.md`.
2. Revise as perguntas abertas em `docs/DECISIONS.md` — várias decisões de produto dependem de confirmação.
3. Nenhuma fase do `docs/ROADMAP.md` é iniciada sem a mensagem "ARQUITETURA APROVADA".
