# DECISIONS — Registro de Decisões, Suposições e Perguntas Abertas

Status: **Vivo** (atualizado a cada fase)
Versão: 0.1.0

Este é o documento central de rastreabilidade de decisões técnicas. Cada entrada referencia onde foi originalmente registrada nos demais documentos. Nada aqui foi decidido "silenciosamente": decisões de produto/negócio permanecem como perguntas abertas até confirmação explícita de Pedro.

## Como ler este documento

- **DECISÃO** — já assumida pelo time técnico para permitir avançar; reversível mediante nova decisão registrada aqui (com data e motivo).
- **SUPOSIÇÃO** — premissa usada na ausência de resposta; tratada como decisão provisória, mas sinalizada para confirmação antes de virar comportamento definitivo no código.
- **PERGUNTA ABERTA** — depende de resposta de Pedro antes de a fase correspondente do `ROADMAP.md` ser iniciada.

## Perguntas abertas (aguardando confirmação) — prioridade para a Fase 0

| # | Pergunta | Onde impacta | Proposta padrão (caso sem resposta) |
|---|---|---|---|
| 1 | Cadastro público (`/auth/register`) é só para STUDENT? INSTRUCTOR é sempre criado/promovido por SUPER_ADMIN? | `API.md` §2.1 | Sim — proposta padrão |
| 2 | Build tool do backend: Maven ou Gradle? | `ARCHITECTURE.md` §3 | Maven |
| 3 | Múltiplas instâncias da API em produção desde o início (afeta necessidade de lock distribuído em jobs)? | `ARCHITECTURE.md` §6 | Assumir que sim e já implementar `FOR UPDATE SKIP LOCKED` (baixo custo) |
| 4 | Upload de vídeo em produção: direto pela API ou presigned URL para o storage? | `ARCHITECTURE.md` §7 | Presigned URL |
| 5 | Provedor de hospedagem de produção (AWS, GCP, Railway, Fly.io, VPS...)? | `ARCHITECTURE.md` §11 | Não definido; arquitetura é agnóstica |
| 6 | Regra exata de conclusão de aula (threshold de % assistido, ex.: 90%?) | `PRD.md` §7 | **DECIDIDO na Fase 4:** ≥90% do vídeo (heartbeat) OU marcação manual; aula sem vídeo só manual; status monotônico |
| 7 | Nota final de um quiz = melhor tentativa ou média das tentativas? | `PRD.md` §8 | Melhor tentativa (`MAX(score)`) |
| 8 | Todo `Quiz` de módulo publicado é obrigatório para certificado, ou há exercícios opcionais? | `PRD.md` §8 | Todos obrigatórios no MVP |
| 9 | Metas numéricas de sucesso do produto (nenhuma foi fornecida) | `PRD.md` §9 | Sem meta fixada; métricas apenas observacionais no MVP |
| 10 | "Despublicar" curso precisa distinguir "nunca publicado" de "já publicado antes"? | `DATABASE.md` §5.3 | Não distinguir no MVP |
| 11 | `Quiz` é por módulo (agregando questões de várias aulas) ou por aula individual? | `DATABASE.md` §5.11 | **DECIDIDO na Fase 5:** por módulo, com `Question.lesson_id` apontando à aula de origem |
| 12 | Histórico completo de mudanças de status de `Enrollment` além do audit log genérico é necessário? | `DATABASE.md` §5.14 | Audit log genérico é suficiente no MVP |
| 13 | Fornecedor concreto de transcrição e de geração de questões (LLM) | `AI_PIPELINE.md` §3 | Não definido; interfaces abstratas permitem decidir depois sem impacto estrutural |
| 14 | Geração parcial (menos questões que o solicitado) é aceita ou o job falha? | `AI_PIPELINE.md` §7 | Aceitar parcial, nunca gerar a mais |
| 15 | Tamanho mínimo/máximo de enunciado e alternativas | `AI_PIPELINE.md` §7 | Proposta: enunciado 10–500 caracteres, alternativa 1–200 caracteres |
| 16 | `max_attempts`/timeout para retomada de job travado de IA | `AI_PIPELINE.md` §13 | 3 tentativas, timeout de 15 min por etapa |
| 17 | Necessidade de aviso contratual ao professor sobre envio de transcrição a provedor terceiro de IA | `AI_PIPELINE.md` §14 | Recomendado constar em termos de uso; fora do escopo técnico do MVP |
| 18 | Algoritmo de assinatura JWT: HS256 (segredo simétrico) ou RS256 (par de chaves)? | `SECURITY.md` §2 | HS256 no MVP |
| 19 | Lista exata de formatos/MIME types aceitos para vídeo e materiais | `SECURITY.md` §5 | A definir na Fase 3 (proposta inicial: `video/mp4` para vídeo; `application/pdf`, imagens comuns para materiais) |
| 20 | Limites exatos de rate limiting (requests/minuto) por endpoint sensível | `API.md` §3, `SECURITY.md` §9 | A definir na Fase 9 (hardening) |
| 21 | Estratégia de gestão de segredos em produção (variáveis de ambiente do provedor vs. secrets manager dedicado) | `SECURITY.md` §4 | Depende da decisão de hospedagem (#5) |
| 22 | Acesso indevido a curso restrito: resposta deve ser 403 ou 404 (para não revelar existência do curso)? | `TEST_STRATEGY.md` §5 | 404 para cursos não publicados/sem acesso, exceto para o próprio dono/admin |
| 23 | Ferramenta de CI | `TEST_STRATEGY.md` §7 | GitHub Actions (repositório já está no GitHub) |
| 24 | Vitest vs. Jest no frontend | `TEST_STRATEGY.md` §4 | Vitest |
| 25 | Meta de cobertura de testes | `TEST_STRATEGY.md` §6 | 80% em módulos de regra de negócio crítica, sem meta rígida em código trivial |

**Ação sugerida:** você não precisa responder todas agora. As perguntas #1, #2, #6, #7, #8, #11, #18 têm maior impacto estrutural e idealmente seriam confirmadas antes do início da Fase 1/2. As demais podem ser confirmadas fase a fase, conforme o `ROADMAP.md` se aproxima delas.

## Suposições assumidas (revisáveis)

| Suposição | Documento de origem | Risco se incorreta |
|---|---|---|
| Usuário pode acumular mais de um papel (ex.: INSTRUCTOR também STUDENT) — modelado como N:N | `PRD.md` §5, `DATABASE.md` §5.2 | Baixo — modelo N:N também funciona com 1 papel por usuário, sem retrabalho |
| Volume de jobs de IA no MVP é baixo (dezenas/dia, não milhares) | `ARCHITECTURE.md` §6 | Médio — exigiria migrar para fila dedicada mais cedo que o previsto |
| Plataforma é single-tenant (uma organização por instalação) no MVP | `PRD.md` §10 | Alto se multi-tenant for necessário logo — exigiria coluna `tenant_id` retroativa em várias tabelas |
| MVP é apenas em pt-BR | `ARCHITECTURE.md` §9 | Baixo — campo `language` já modelado para expansão futura |
| Exclusão física de `Question` só é permitida sem `StudentAnswer` associada | `DATABASE.md` §3 | Baixo — regra conservadora, fácil de flexibilizar depois |

## Decisões técnicas confirmadas (não dependem de aprovação de produto)

Resumo das decisões arquiteturais registradas em detalhe nos respectivos documentos — listadas aqui para referência rápida:

1. Monorepo com `apps/web` (Next.js) e `apps/api` (Spring Boot), deployáveis independentemente — `ARCHITECTURE.md` §1–2.
2. Subsistema de IA isolado por interfaces (`TranscriptionProvider`, `QuestionGenerationProvider`, `AiContentValidator`, `AiUsageTracker`) — `ARCHITECTURE.md` §5, `AI_PIPELINE.md` §3.
3. UUID v4 como chave primária de todas as tabelas de domínio; `Certificate.validation_code` como código curto separado — `DATABASE.md` §1.
4. Enums de domínio como `VARCHAR + CHECK CONSTRAINT`, não `ENUM` nativo do Postgres — `DATABASE.md` §2.
5. Soft delete para entidades com histórico acadêmico/auditoria; exclusão física restrita a tabelas de vínculo sem histórico próprio — `DATABASE.md` §3.
6. Índice único parcial garantindo no máximo uma alternativa correta por questão a nível de banco — `DATABASE.md` §5.13.
7. Sem message broker dedicado no MVP; jobs de IA via tabela de estado + `@Async` + agendador com `FOR UPDATE SKIP LOCKED` — `ARCHITECTURE.md` §6.
8. Abstração `VideoStorageProvider` com implementação local de dev e S3-compatible/streaming para produção; nenhum binário em coluna de banco — `ARCHITECTURE.md` §7.
9. JWT de curta duração + refresh token em cookie `httpOnly`; access token nunca em `localStorage` — `ARCHITECTURE.md` §8, `SECURITY.md` §2.
10. Formato de erro padronizado (Problem Details), sem stack trace exposto — `API.md` §1, `SECURITY.md` §6.
11. Regra inegociável: nenhuma questão `AI_GENERATED` alcança `PUBLISHED` sem aprovação humana — `AI_PIPELINE.md` §10.
12. Correção de tentativas é 100% determinística no backend, sem nova chamada à IA — `AI_PIPELINE.md` §11.
13. Idempotência obrigatória em `AiGenerationJob` via `idempotency_key` único — `AI_PIPELINE.md` §8.
14. Dados do certificado são snapshot imutável no momento da emissão — `DATABASE.md` §5.18.
15. Nenhum seed de dados fictícios em produção; seeds restritos a perfis `dev`/`local` — `SECURITY.md` §8.
16. Flyway como única ferramenta de migração; `ddl-auto` do Hibernate desabilitado em todos os ambientes — `DATABASE.md` §7.
17. shadcn/ui + Tailwind como base de componentes do frontend — `ARCHITECTURE.md` §3.

## Decisões reais da Fase 1 (implementação — 2026-08-06)

A implementação da Fase 1 ("Fundação": monorepo, auth, RBAC — nomeada assim no prompt de execução, cobrindo o que este documento originalmente separava em "Fase 0" e "Fase 1" do `ROADMAP.md`) resolveu e/ou confirmou os seguintes pontos:

| # | Item | Resolução |
|---|---|---|
| Pergunta #2 | Build tool do backend | **DECIDIDO: Maven** (proposta padrão confirmada) |
| Pergunta #18 | Algoritmo JWT | **DECIDIDO: HS256** (proposta padrão confirmada) — segredo mínimo de 32 bytes validado em runtime (`JwtService`); a aplicação recusa subir sem `JWT_SECRET` válido |
| Pergunta #24 | Vitest vs. Jest no frontend | **DECIDIDO: Vitest** (proposta padrão confirmada), com Testing Library para componentes |
| Pergunta #1 | Cadastro público só para STUDENT | **DECIDIDO conforme proposta:** `POST /auth/register` sempre atribui papel `STUDENT`; papéis `INSTRUCTOR`/`SUPER_ADMIN` só são atribuídos por um `SUPER_ADMIN` via `POST /users/{id}/roles` |
| — | Versão do Next.js | **DESVIO do plano original (Next 14 → Next 16.3.0)** — ver `ARCHITECTURE.md` §3 para justificativa. Nenhum requisito funcional foi impactado; App Router e o restante da stack (Tailwind, shadcn/ui, RHF+Zod) seguem como planejado |
| — | Proteção de rota no frontend | Implementada 100% client-side via `AuthProvider` + `ProtectedRoute`, dado que o cookie `httpOnly` do refresh token pertence à origem da API e não é legível por um middleware Next.js rodando em outra origem/porta em ambiente de desenvolvimento local. Detalhe completo em `ARCHITECTURE.md` §8 |
| — | Geração de cliente HTTP a partir do OpenAPI | Adiada: Fase 1 usa um wrapper `fetch` central escrito à mão (`apps/web/src/lib/api-client.ts`), incluindo retry automático de uma única tentativa após renovação de token em respostas 401. Geração automática a partir do `springdoc-openapi` fica para quando o contrato de API estabilizar (reduz risco de churn de código gerado nesta fase inicial) |
| — | Endpoints de auth publicamente acessíveis | `register`, `login`, `refresh`, `logout`, `password/forgot`, `password/reset` são as únicas rotas de `/api/v1/auth/**` com `permitAll()`; `GET /auth/me` e `POST /auth/password/change` exigem autenticação (reforçado também com `@PreAuthorize("isAuthenticated()")` no controller, como defesa em profundidade contra erro de configuração do `SecurityConfig`) |
| — | Charset das respostas de erro de segurança | `RestAuthenticationEntryPoint` e `RestAccessDeniedHandler` fixam `response.setCharacterEncoding("UTF-8")` explicitamente antes de escrever o corpo JSON — o padrão do Tomcat (`ISO-8859-1`) corrompia acentuação em mensagens de erro em português |
| — | Ambiente de execução de testes de integração/Docker | O sandbox usado durante a implementação **não tem Docker disponível** (nem `dockerd`, nem `docker compose`). Testes de integração com Testcontainers (`AuthControllerIT`, `AuthorizationIT`) foram escritos e compilam, mas não foram executados neste ambiente — apenas os testes unitários (`mvn test`, 20 testes) rodaram de fato. `docker-compose.yml`/`Dockerfile`s foram escritos seguindo boas práticas (multi-stage build, usuário não-root, saída `standalone` do Next.js) mas não puderam ser buildados/executados aqui. Recomenda-se rodar `mvn verify` e `docker compose up --build` em um ambiente com Docker antes do deploy |

## Decisões do sistema visual (Design System)

Escopo: exclusivamente visual/UX (`apps/web`), sem alteração de regras de negócio, contratos de API, banco de dados ou funcionalidades. Fonte da verdade: `docs/DESIGN_SYSTEM.md` — **Official Brand Color System — v1**.

| # | Item | Resolução |
|---|---|---|
| — | Combinação tipográfica | **DECIDIDO:** Fraunces (heading) + Public Sans (interface) + Geist Mono (código), via `next/font/google` |
| — | Identidade cromática oficial (v1, 2026-08-07) | **INEGOCIÁVEL:** navy `#040A16` + gold champagne `#BA9364`. Distribuição ~80% navy / 15% texto / 5% dourado. HEX da paleta não podem ser reinterpretados sem nova decisão explícita |
| — | Tema claro legado (off-white + verde) | **SUBSTITUÍDO** pela v1 navy+gold; app é dark-first único |
| — | Itens de menu ainda não implementados (Certificados, Configurações) | **DECIDIDO:** selo "Em breve". **Processamentos de IA** e **Meus cursos** são rotas reais |
| — | Métricas do dashboard sem dado real | **DECIDIDO:** placeholders explícitos; nunca número inventado |

## Decisões reais da Fase 3 (vídeos + IA — implementação, 2026-08-06)

| # | Item | Resolução |
|---|---|---|
| — | Escopo vs. ROADMAP original | **DECIDIDO pelo prompt do usuário:** entregar o fluxo vertical (vídeo → transcrição → geração → revisão) numa única fase, unificando o que o ROADMAP separava em 3/6/7 |
| — | Provedores de IA no MVP | **DECIDIDO: mock plugável** (`MockTranscriptionProvider`, `MockQuestionGenerationProvider`) atrás das interfaces; troca futura = novo bean + `app.ai.provider` |
| — | E2E sem arquivo de vídeo | **DECIDIDO:** `devTranscriptText` só no perfil `dev` — cria stub de `VideoAsset` e alimenta a transcrição para teste ponta a ponta |
| — | Publicação de questões de IA | **DECIDIDO (inegociável):** `AI_GENERATED` só chega a `PUBLISHED` após `APPROVED` por humano; `raw_ai_payload` nunca é sobrescrito na edição |
| — | Dashboard demonstrativo anterior | **DECIDIDO:** resetado para dados reais da Fase 2; mocks de alunos/certificados/IA removidos da home autenticada |
| — | Depoimentos e conteúdo social da landing | **DECIDIDO:** nenhum depoimento, nome de aluno ou número de negócio foi inventado; a seção existe como espaço reservado explícito até haver conteúdo real |
| — | Preview de curso na landing pública | **DECIDIDO:** dado ilustrativo, rotulado como "Exemplo" na própria UI — a listagem pública real de cursos publicados depende de um endpoint público que ainda não existe (endpoint atual de cursos exige autenticação) |

## Decisões reais da Fase 4 (matrículas e progresso — implementação, 2026-08-07)

| # | Item | Resolução |
|---|---|---|
| Pergunta #6 | Conclusão de aula | **DECIDIDO:** `COMPLETED` quando `last_position_seconds >= ceil(90% * duration)` via heartbeat, ou `POST .../complete` manual; sem vídeo → só manual; não regride de `COMPLETED` |
| — | Acesso sem matrícula | **DECIDIDO: 403** (critério do ROADMAP), mensagem explícita de matrícula necessária |
| — | `FREE_PREVIEW` | **DECIDIDO:** aula publicada em curso publicado libera `stream-url`/materiais sem matrícula; `ENROLLED_ONLY` exige `Enrollment.status = ACTIVE` |
| — | Modelo de matrícula | **DECIDIDO:** `UNIQUE(student, course)`; suspend/cancel/reativar = mudança de `status` + `AuditLog` (sem histórico versionado na linha) |
| — | Quem matricula | **DECIDIDO:** `SUPER_ADMIN` ou `INSTRUCTOR` dono; aluno não se auto-matricula no MVP |
| — | UI | **DECIDIDO:** painel de matrículas no construtor do curso; aluno em `/my-courses` + player com heartbeat |

## Decisões reais da Fase 5 (exercícios manuais e tentativas — implementação, 2026-08-07)

| # | Item | Resolução |
|---|---|---|
| Pergunta #11 | Quiz por módulo vs. por aula | **CONFIRMADO:** 1 quiz por módulo; `Question.lessonId` aponta a aula de origem |
| — | Estrutura da questão manual | **DECIDIDO:** exatamente 4 alternativas e exatamente 1 correta (`QuestionStructureRules`) |
| — | Correção | **DECIDIDO:** determinística no submit; `score = correct/total_published × 100` (2 casas); `passed` usa `quiz.passingScore` ou `course.minPassingScore` |
| — | `max_attempts` | **DECIDIDO:** override no quiz; senão `course.maxQuizAttempts`; `null` = ilimitado |
| — | Imutabilidade | **DECIDIDO:** após `GRADED`, `answers`/`submit` retornam 400 |
| — | Gate | **DECIDIDO:** take/attempt exige matrícula `ACTIVE` (mesmo padrão da Fase 4) |
| — | IA | **FORA DE ESCOPO nesta fase:** criação manual apenas; pipeline mock permanece para jobs existentes |

## Decisões reais da Fase 6 (pipeline de IA — fechamento, 2026-08-07)

| # | Item | Resolução |
|---|---|---|
| — | Escopo vs. Fase 3 | **DECIDIDO:** Fase 3 já entregou o fluxo vertical + mocks + revisão; Fase 6 fecha reclaim, resume e testes de aceite |
| — | Jobs travados | **DECIDIDO:** scheduler periódico + `FOR UPDATE SKIP LOCKED`; timeout `app.ai.stuck-timeout-minutes` (15); `max-attempts` (3) |
| — | Retomada | **DECIDIDO:** `POST /ai-jobs/{id}/resume` para `FAILED`; se já há questões do job → `AWAITING_REVIEW` sem regenerar |
| — | Provedores reais | **MANTIDO mock** — sem gasto em LLM nesta fase |
| — | Publicação AI | **CONFIRMADO:** exige `APPROVED` + `approved_by_user_id` (guard + teste) |

## Limitações conhecidas do MVP (aceitas conscientemente)

- Sem pagamento/assinatura — acesso é 100% manual (`Enrollment` criado por admin/instrutor).
- Sem fila de mensageria dedicada — throughput de IA limitado à capacidade de uma instância com `@Async` (mitigável, não implementado no MVP).
- Sem multi-tenancy — uma instalação serve uma única organização/marca.
- Sem apps mobile nativos — apenas web responsivo.
- Sem observabilidade avançada (APM) definida — apenas logs estruturados e `AuditLog`.
- Sem metas numéricas de performance/cobertura de teste fixadas pelo usuário — propostas incluídas neste documento como ponto de partida.

## Histórico de mudanças deste documento

| Data | Mudança |
|---|---|
| 2026-08-06 | Criação inicial junto com o restante da documentação técnica (PRD, ARCHITECTURE, DATABASE, API, AI_PIPELINE, SECURITY, ROADMAP, TEST_STRATEGY) |
| 2026-08-06 | Adicionada seção "Decisões reais da Fase 1 (implementação)" após "ARQUITETURA APROVADA" e execução da Fase 1 (fundação, autenticação e RBAC) |
| 2026-08-06 | Adicionada seção "Decisões do sistema visual (Design System)" após a implementação da identidade visual oficial da plataforma (paleta, tipografia, componentes, estrutura das três experiências) |
| 2026-08-06 | Adicionada seção "Decisões reais da Fase 3" (vídeos, storage local, pipeline de IA mock, revisão humana, reset do dashboard demo) |
| 2026-08-07 | Adicionada seção "Decisões reais da Fase 4" (matrículas, progresso 90%/manual, gate de vídeo, área do aluno); pergunta #6 fechada |
| 2026-08-07 | Adicionada seção "Decisões reais da Fase 5" (quiz manual, tentativas, score, max_attempts); pergunta #11 fechada |
| 2026-08-07 | Official Brand Color System — v1 (navy `#040A16` + gold `#BA9364`); `DESIGN_SYSTEM.md` reescrito; paleta anterior off-white/verde aposentada |
| 2026-08-07 | Fase 6 fechada (resume, stuck recovery SKIP LOCKED, testes idempotência/aprovação); Fase 2 marcada concluída no ROADMAP |
