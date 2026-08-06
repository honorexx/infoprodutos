# API — Contrato e Convenções

Status: **Rascunho para aprovação**
Versão: 0.1.0

Este documento define **convenções** e o **mapa de recursos** do MVP. O contrato definitivo (schemas exatos de request/response) será gerado via **springdoc-openapi** a partir do código na Fase 0/1 e publicado em `/api-docs` (dev) — este arquivo não substitui o OpenAPI gerado, serve como guia de design antes da implementação.

## 1. Convenções gerais

- **[DECISÃO]** Base path: `/api/v1`. Versionamento por prefixo de URL (`v1`, `v2`...) em vez de header, por simplicidade de depuração e cache.
- Formato: JSON (`application/json`) em todas as requisições e respostas.
- Autenticação: `Authorization: Bearer <access_token>` (JWT) para chamadas autenticadas; refresh token via cookie `httpOnly` em endpoint dedicado.
- Identificadores em URLs e payloads são UUIDs (string).
- Datas em ISO-8601 UTC (`2026-08-06T12:00:00Z`).
- Paginação: `?page=0&size=20&sort=campo,asc` (padrão Spring Data `Pageable`), resposta no formato:

```json
{
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 123,
  "totalPages": 7
}
```

- **[DECISÃO]** Formato de erro padronizado (baseado em RFC 7807 — Problem Details):

```json
{
  "type": "https://docs.internal/errors/validation-error",
  "title": "Erro de validação",
  "status": 400,
  "detail": "Mensagem segura para o usuário final",
  "instance": "/api/v1/courses",
  "errors": [
    { "field": "title", "message": "não pode ser vazio" }
  ]
}
```

Stack traces e detalhes internos **nunca** são incluídos na resposta (ver `SECURITY.md`). Erros 5xx retornam mensagem genérica; detalhes vão para log estruturado no servidor com um `correlationId` retornado ao cliente para suporte.

- Autorização: cada endpoint documenta os papéis permitidos. Verificação de posse (ex.: "este curso pertence a este instrutor?") é sempre feita no serviço, não apenas por papel.

## 2. Mapa de recursos (MVP)

Esta é uma visão de alto nível dos grupos de endpoints; não é exaustiva de query params/filtros, que serão detalhados no OpenAPI durante a implementação de cada fase.

### 2.1 Autenticação (`/api/v1/auth`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| POST | `/auth/register` | público | Cadastro de aluno (**[PERGUNTA ABERTA]**: cadastro público é só para STUDENT? Professores são criados por SUPER_ADMIN? — proposta: sim, INSTRUCTOR só é criado/promovido por SUPER_ADMIN) |
| POST | `/auth/login` | público | Autenticação, retorna access token + seta cookie de refresh |
| POST | `/auth/logout` | autenticado | Invalida refresh token corrente |
| POST | `/auth/refresh` | público (via cookie) | Renova access token |
| POST | `/auth/password/forgot` | público | Envia e-mail de recuperação de senha |
| POST | `/auth/password/reset` | público (via token do e-mail) | Define nova senha |
| POST | `/auth/password/change` | autenticado | Troca senha estando logado |
| GET | `/auth/me` | autenticado | Retorna usuário atual + papéis |

### 2.2 Usuários (`/api/v1/users`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| GET | `/users` | SUPER_ADMIN | Lista usuários (paginado, filtro por papel/status) |
| GET | `/users/{id}` | SUPER_ADMIN, próprio usuário | Detalhe |
| PATCH | `/users/{id}` | SUPER_ADMIN, próprio usuário (campos limitados) | Atualiza dados |
| POST | `/users/{id}/block` | SUPER_ADMIN | Bloqueio lógico (`status = BLOCKED`) |
| POST | `/users/{id}/unblock` | SUPER_ADMIN | Reativação |
| POST | `/users/{id}/roles` | SUPER_ADMIN | Atribui papel (ex.: promover a INSTRUCTOR) |

### 2.3 Cursos (`/api/v1/courses`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| GET | `/courses` | autenticado (escopo varia) | SUPER_ADMIN vê todos; INSTRUCTOR vê os seus; STUDENT vê cursos publicados/matriculados |
| GET | `/courses/{id}` | conforme regra de acesso | Detalhe do curso |
| POST | `/courses` | SUPER_ADMIN, INSTRUCTOR | Cria curso (`status = DRAFT`) |
| PUT | `/courses/{id}` | SUPER_ADMIN, dono (INSTRUCTOR) | Edita informações gerais |
| POST | `/courses/{id}/publish` | SUPER_ADMIN, dono | Publica |
| POST | `/courses/{id}/unpublish` | SUPER_ADMIN, dono | Despublica (volta a `DRAFT`) |
| POST | `/courses/{id}/archive` | SUPER_ADMIN, dono | Arquiva |
| DELETE | `/courses/{id}` | SUPER_ADMIN | Soft delete |
| GET | `/courses/{id}/metrics` | SUPER_ADMIN, dono | Quantidade de alunos e % média de conclusão |

### 2.4 Módulos (`/api/v1/courses/{courseId}/modules`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| GET | `/modules` | conforme acesso ao curso | Lista módulos (com aulas resumidas) |
| POST | `/modules` | SUPER_ADMIN, dono do curso | Cria módulo |
| PUT | `/modules/{id}` | SUPER_ADMIN, dono | Edita |
| DELETE | `/modules/{id}` | SUPER_ADMIN, dono | Exclui quando permitido (sem aulas com progresso registrado — regra a validar no serviço) |
| POST | `/modules/reorder` | SUPER_ADMIN, dono | Recebe lista ordenada de IDs, reatribui `order_index` transacionalmente |
| POST | `/modules/{id}/publish` | SUPER_ADMIN, dono | Publica módulo |

### 2.5 Aulas (`/api/v1/modules/{moduleId}/lessons`)

Estrutura análoga a módulos: `GET`, `POST`, `PUT`, `DELETE`, `/reorder`, `/publish`. Adicionalmente:

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| POST | `/lessons/{id}/video` | SUPER_ADMIN, dono | Inicia associação/upload de vídeo (ver §2.6) |
| POST | `/lessons/{id}/materials` | SUPER_ADMIN, dono | Adiciona material complementar |
| DELETE | `/lessons/{id}/materials/{materialId}` | SUPER_ADMIN, dono | Remove material |

### 2.6 Vídeos (`/api/v1/videos`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| POST | `/videos/upload-init` | SUPER_ADMIN, INSTRUCTOR (dono da aula) | Cria `VideoAsset` (`PENDING`) e retorna instruções de upload (dev: endpoint direto; prod: presigned URL) |
| POST | `/videos/{id}/upload-complete` | idem | Confirma que o upload terminou, dispara validação/processamento |
| GET | `/videos/{id}/stream-url` | matriculado no curso da aula, ou dono/admin | Retorna URL assinada de curta duração para reprodução — **nunca** a URL bruta do storage |
| DELETE | `/videos/{id}` | SUPER_ADMIN, dono | Remove associação (não apaga histórico, ver `DATABASE.md` §5.7) |

### 2.7 Matrículas (`/api/v1/enrollments`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| GET | `/enrollments` | SUPER_ADMIN, INSTRUCTOR (dos seus cursos) | Lista matrículas com filtro por curso/aluno/status |
| POST | `/enrollments` | SUPER_ADMIN, INSTRUCTOR (dos seus cursos) | Concede acesso manual |
| POST | `/enrollments/{id}/suspend` | SUPER_ADMIN, INSTRUCTOR dono | Suspende acesso |
| POST | `/enrollments/{id}/cancel` | SUPER_ADMIN, INSTRUCTOR dono | Cancela |
| GET | `/enrollments/me` | STUDENT | Minhas matrículas |

### 2.8 Progresso (`/api/v1/enrollments/{enrollmentId}/progress`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| POST | `/lessons/{lessonId}/start` | STUDENT dono da matrícula | Marca aula como iniciada |
| POST | `/lessons/{lessonId}/heartbeat` | STUDENT dono | Atualiza `last_position_seconds` periodicamente |
| POST | `/lessons/{lessonId}/complete` | STUDENT dono | Marca aula concluída (manual ou automática por threshold) |
| GET | `/summary` | STUDENT dono, ou INSTRUCTOR/SUPER_ADMIN do curso | Progresso agregado por módulo/curso |

**[DECISÃO]** Toda escrita de progresso valida no backend que `enrollment.student_user_id == usuário autenticado` — um aluno nunca pode alterar progresso de outro, mesmo manipulando o `enrollmentId` na URL (checagem de posse obrigatória).

### 2.9 Exercícios / Quizzes (`/api/v1/quizzes`, `/api/v1/questions`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| GET | `/modules/{moduleId}/quiz` | conforme acesso | Detalhe do quiz do módulo |
| POST | `/quizzes/{id}/questions` | SUPER_ADMIN, INSTRUCTOR dono | Cria questão manual (`origin = MANUAL`) |
| PUT | `/questions/{id}` | SUPER_ADMIN, INSTRUCTOR dono | Edita questão (inclusive as geradas por IA, antes ou depois de aprovar) |
| POST | `/questions/{id}/approve` | SUPER_ADMIN, INSTRUCTOR dono | Aprova questão gerada por IA |
| POST | `/questions/{id}/reject` | SUPER_ADMIN, INSTRUCTOR dono | Rejeita |
| POST | `/questions/bulk-approve` | idem | Aprovação em massa |
| DELETE | `/questions/{id}` | SUPER_ADMIN, INSTRUCTOR dono | Remove (regra de soft delete condicional, ver `DATABASE.md`) |
| GET | `/quizzes/{id}/attempt` | STUDENT matriculado | Retorna questões publicadas para responder (sem `is_correct` exposto) |

### 2.10 Tentativas (`/api/v1/quiz-attempts`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| POST | `/quizzes/{quizId}/attempts` | STUDENT matriculado | Inicia nova tentativa (valida `max_attempts`) |
| POST | `/quiz-attempts/{id}/answers` | STUDENT dono da tentativa | Registra resposta a uma questão |
| POST | `/quiz-attempts/{id}/submit` | STUDENT dono | Finaliza e corrige deterministicamente no backend |
| GET | `/quiz-attempts/{id}` | STUDENT dono, ou INSTRUCTOR/SUPER_ADMIN do curso | Detalhe (resultado, respostas, explicações conforme config do curso) |
| GET | `/quiz-attempts` | STUDENT (próprias) / INSTRUCTOR (do curso) | Histórico |

### 2.11 Certificados (`/api/v1/certificates`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| GET | `/certificates/me` | STUDENT | Meus certificados |
| POST | `/enrollments/{id}/certificate/issue` | STUDENT dono (auto-elegibilidade) ou sistema | Emite certificado se critérios cumpridos |
| GET | `/certificates/{id}/pdf` | STUDENT dono, SUPER_ADMIN | Download do PDF |
| GET | `/public/certificates/validate/{validationCode}` | **público** | Validação pública — retorna apenas nome do aluno, curso, professor, carga horária, data e status (válido/revogado); nenhum dado sensível adicional |

### 2.12 IA — jobs e revisão (`/api/v1/ai`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| POST | `/lessons/{lessonId}/ai-jobs` | SUPER_ADMIN, INSTRUCTOR dono | Solicita geração (transcrição + questões), com `idempotencyKey` |
| GET | `/ai-jobs/{id}` | SUPER_ADMIN, INSTRUCTOR dono | Status do job |
| GET | `/ai-jobs` | SUPER_ADMIN (todos), INSTRUCTOR (dos seus cursos) | Lista/monitoramento (tela "Processamentos de IA") |
| POST | `/ai-jobs/{id}/cancel` | SUPER_ADMIN, INSTRUCTOR dono | Cancela job pendente |
| POST | `/ai-jobs/{id}/regenerate` | SUPER_ADMIN, INSTRUCTOR dono | Solicita nova geração (novo job, mantendo idempotência) |
| GET | `/ai-jobs/{id}/reviews` | idem | Lista questões geradas pendentes de revisão + evidência |

### 2.13 Auditoria (`/api/v1/audit-logs`)

| Método | Rota | Papel | Descrição |
|---|---|---|---|
| GET | `/audit-logs` | SUPER_ADMIN | Consulta de auditoria, filtros por entidade/ator/período |

## 3. Rate limiting — endpoints sensíveis

**[DECISÃO]** Aplicar limite de requisições (ex.: via `bucket4j` ou filtro próprio) pelo menos em:
- `/auth/login`, `/auth/password/forgot`, `/auth/register` (mitigar força bruta/enumeração).
- `/videos/{id}/stream-url` (mitigar abuso de geração de URLs assinadas).
- `/lessons/{id}/ai-jobs` (custo de IA — evitar disparo repetido acidental ou malicioso).

Limites exatos (requests/minuto) — **[PERGUNTA ABERTA]**, a definir na Fase de Segurança/Hardening.

## 4. Documentos relacionados

`AI_PIPELINE.md` detalha os payloads de entrada/saída da IA. `SECURITY.md` detalha modelo de autorização e tratamento de erros. `DATABASE.md` é a fonte de verdade dos campos por entidade.
