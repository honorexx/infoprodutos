# ARCHITECTURE — Arquitetura Técnica

Status: **Rascunho para aprovação**
Versão: 0.1.0

Convenções `[DECISÃO]` / `[SUPOSIÇÃO]` / `[PERGUNTA ABERTA]` conforme definidas em `PRD.md`.

## 1. Estilo arquitetural

**[DECISÃO]** Monorepo com dois aplicativos (`web` e `api`) e um subsistema de IA **isolado logicamente** dentro da API (não como domínio acoplado). Justificativa:
- MVP com equipe pequena: um monorepo reduz overhead operacional versus múltiplos repositórios.
- O subsistema de IA precisa ser substituível (múltiplos provedores) sem vazar detalhes de fornecedor para o domínio de cursos — por isso vive atrás de interfaces (`TranscriptionProvider`, `QuestionGenerationProvider`, `AiContentValidator`, `AiUsageTracker`), mesmo rodando no mesmo processo/deploy do backend principal no MVP.
- Não adotamos microsserviços no MVP: a complexidade operacional (orquestração, observabilidade distribuída) não se justifica para o estágio atual do produto. A arquitetura é modular internamente para permitir extração futura de serviços (ex.: worker de IA como serviço separado) sem reescrever o domínio.

## 2. Estrutura de monorepo

```
/apps
  /web            # Next.js (App Router) — painel admin, área do professor, área do aluno, página pública de certificado
  /api            # Spring Boot — domínio, autenticação, subsistema de IA, jobs assíncronos
/docs             # Documentação técnica (este conjunto de arquivos)
/infra
  docker-compose.yml   # Postgres + api + web para ambiente local
  ...configurações locais (ex.: pasta de armazenamento de vídeo local)
```

**[DECISÃO]** `apps/web` e `apps/api` são deployáveis de forma independente (dois processos/containers), mesmo vivendo no mesmo monorepo. Isso evita acoplar o ciclo de release do frontend ao backend.

## 3. Stack tecnológica e versões

**[DECISÃO — pinado na Fase 1, ver `DECISIONS.md` §"Decisões reais da Fase 1"]** Versões efetivamente implementadas:

| Camada | Tecnologia | Versão alvo | Versão real (Fase 1) |
|---|---|---|---|
| Frontend | Next.js | 14.x (App Router) | **16.3.0** (App Router) — ver nota de desvio abaixo |
| Frontend | React | 18.x | **19.2.8** (versão exigida pelo Next.js 16) |
| Frontend | TypeScript | 5.4+ | 5.9.3 |
| Frontend | Tailwind CSS | 3.4+ | **4.x** (instalado pelo `create-next-app`/shadcn atuais) |
| Frontend | Componentes | shadcn/ui (Radix UI + Tailwind) | shadcn/ui (preset Nova, base Radix) |
| Frontend | Formulários/validação | React Hook Form + Zod | React Hook Form 7 + Zod 4 |
| Frontend | Cliente HTTP | Cliente tipado a partir do OpenAPI | **Fase 1: wrapper `fetch` central manual** (`src/lib/api-client.ts`), com refresh automático em 401. Geração a partir do OpenAPI fica para fase futura, quando o contrato estabilizar. |
| Frontend | Gerenciador de pacotes | pnpm (workspaces) | pnpm (sem workspaces ainda — apenas `apps/web` tem `package.json` na Fase 1) |
| Backend | Java | 21 (LTS) | 21 (LTS) |
| Backend | Spring Boot | 3.3.x | **3.5.16** |
| Backend | Spring Security | incluso no Boot 3.3.x | incluso no Boot 3.5.16 |
| Backend | Spring Data JPA / Hibernate | incluso no Boot 3.3.x | incluso no Boot 3.5.16 |
| Backend | Bean Validation | Hibernate Validator (Jakarta Validation 3.x) | Hibernate Validator (Jakarta Validation 3.x) |
| Backend | Migrações | Flyway 10.x | Flyway (gerenciado pelo Spring Boot 3.5.16) |
| Backend | Documentação de API | springdoc-openapi 2.x | springdoc-openapi 2.8.17 |
| Backend | JWT | — | jjwt 0.12.6 (HS256) |
| Backend | Testes | JUnit 5, Mockito 5, Testcontainers 1.19+ | JUnit 5, Mockito 5, Testcontainers 1.20.4 |
| Backend | Build | Maven (multi-módulo) ou Gradle — **[PERGUNTA ABERTA]** | **DECIDIDO: Maven** (pergunta #2 de `DECISIONS.md` resolvida) |
| Banco | PostgreSQL | 16.x | 16 (`postgres:16-alpine` no Docker Compose) |
| Infra local | Docker Compose | — | `docker-compose.yml` na raiz (Postgres + api + web) |

**Nota de desvio — versão do Next.js:** o prompt de implementação da Fase 1 mencionava Next.js 14; como o `create-next-app` estável no momento da implementação já instala a linha 16.x (com React 19), optou-se por seguir a versão estável mais recente em vez de fixar manualmente uma versão desatualizada, evitando dívida técnica imediata. A API pública usada (App Router, Server/Client Components, Route Handlers) é compatível com o que foi planejado; nenhuma decisão de arquitetura deste documento dependia especificamente da versão 14. Se houver motivo de negócio para fixar a v14, é uma mudança de baixo custo nesta fase inicial.

**[SUPOSIÇÃO]** shadcn/ui foi escolhido por ser acessível (baseado em Radix), altamente customizável (código copiado para o projeto, sem "cara de template genérico") e compatível com Tailwind — atende ao requisito de UI "profissional, moderna, sem aparência genérica". Alternativa considerada e descartada por ora: Material UI (visual mais "padrão", menos flexível para identidade própria).

## 4. Diagrama de arquitetura (visão de componentes)

```mermaid
graph TB
  Browser["Navegador do usuário<br/>(admin, professor, aluno, visitante)"]

  subgraph WebApp["apps/web — Next.js"]
    UIAdmin["Painel Admin"]
    UIInstructor["Área do Professor"]
    UIStudent["Área do Aluno"]
    UIPublic["Página pública de validação de certificado"]
  end

  subgraph ApiApp["apps/api — Spring Boot"]
    Gateway["Camada REST (Controllers)"]
    Security["Spring Security<br/>(JWT, RBAC, method security)"]
    Domain["Domínio: Courses, Modules, Lessons,<br/>Enrollments, Progress, Quizzes, Certificates"]
    StorageAbstraction["Storage Abstraction<br/>(VideoStorageProvider)"]
    AiSubsystem["Subsistema de IA<br/>(interfaces + implementações)"]
    JobRunner["Job Runner assíncrono<br/>(AiGenerationJob state machine)"]
    Audit["Audit Logging"]
  end

  DB[("PostgreSQL")]

  subgraph ExternalProd["Integrações externas (produção)"]
    ObjectStorage["Armazenamento compatível S3<br/>ou serviço de streaming"]
    AIProviderT["Provedor de Transcrição"]
    AIProviderG["Provedor de Geração de Questões (LLM)"]
  end

  LocalDevStorage["Armazenamento local de dev<br/>(filesystem)"]

  Browser --> UIAdmin
  Browser --> UIInstructor
  Browser --> UIStudent
  Browser --> UIPublic

  UIAdmin --> Gateway
  UIInstructor --> Gateway
  UIStudent --> Gateway
  UIPublic --> Gateway

  Gateway --> Security
  Security --> Domain
  Domain --> DB
  Domain --> StorageAbstraction
  Domain --> Audit
  Audit --> DB

  StorageAbstraction -.dev.-> LocalDevStorage
  StorageAbstraction -.prod.-> ObjectStorage

  Domain --> JobRunner
  JobRunner --> AiSubsystem
  AiSubsystem --> AIProviderT
  AiSubsystem --> AIProviderG
  AiSubsystem --> DB
```

## 5. Subsistema de IA — princípio de isolamento

**[DECISÃO]** O domínio de cursos nunca chama um SDK de provedor de IA diretamente. Toda comunicação passa por interfaces definidas no módulo `ai` da API:

```
TranscriptionProvider        -> transcreve um VideoAsset em Transcript + TranscriptSegment[]
QuestionGenerationProvider    -> gera questões estruturadas a partir de uma transcrição
AiContentValidator            -> valida estrutural e semanticamente a saída da IA
AiUsageTracker                 -> registra consumo/custo por job, para auditoria e limites
```

Isso permite trocar o provedor concreto (ex.: outro fornecedor de transcrição ou de LLM) sem alterar o domínio de cursos, exercícios ou revisão. Detalhamento completo em `AI_PIPELINE.md`.

## 6. Processamento assíncrono

**[DECISÃO — MVP]** Sem message broker dedicado (Kafka/RabbitMQ/SQS) no MVP. Usamos:
- Tabela `ai_generation_job` como fonte de verdade do estado (state machine persistida).
- `@Async` do Spring + um `TaskExecutor` dedicado para dar início ao processamento sem bloquear a requisição HTTP.
- Um agendador (`@Scheduled`) para retomar jobs travados/retentar falhas transitórias, respeitando `attempt_count` e backoff.

Justificativa: reduz complexidade operacional para a escala inicial (poucos professores, geração sob demanda, não é streaming de eventos de alto volume). **[SUPOSIÇÃO]** Volume esperado no MVP é baixo (dezenas de jobs/dia, não milhares). Se este pressuposto mudar, a migração para uma fila real (ex.: SQS, RabbitMQ) é viável sem reescrever o domínio, pois o `JobRunner` já é uma camada isolada.

**[PERGUNTA ABERTA]** Confirmar se have múltiplas instâncias da API rodarão em paralelo em produção desde o início — isso afeta se é necessário lock distribuído (ex.: `SELECT ... FOR UPDATE SKIP LOCKED`) para o agendador não processar o mesmo job duas vezes. Proposta: já implementar `FOR UPDATE SKIP LOCKED` na consulta de jobs pendentes, mesmo com uma única instância, por ser barato e evitar retrabalho futuro.

## 7. Armazenamento de vídeo

**[DECISÃO]** Abstração `VideoStorageProvider` com pelo menos duas implementações:

- **Local (dev)**: grava arquivo no filesystem local (pasta configurável via variável de ambiente, ex.: `./infra/local-storage/videos`), servido por um endpoint autenticado da API que verifica matrícula antes de liberar o stream (sem URL pública direta).
- **Produção**: implementação compatível com S3 (AWS S3, Cloudflare R2, MinIO etc.) usando **URLs assinadas de curta duração** para leitura, geradas sob demanda após verificação de matrícula/autorização. **[PERGUNTA ABERTA]**: confirmar se produção usará upload direto para o storage (presigned upload URL) ou upload via API — proposta padrão: presigned upload URL para evitar tráfego de binários grandes pela API.
- Interface deixa espaço para uma terceira implementação futura (serviço especializado de streaming, ex.: Mux/Cloudflare Stream) sem alterar o domínio de `Lesson`/`VideoAsset`.

Nenhum binário de vídeo é armazenado no PostgreSQL.

## 8. Autenticação e sessão

**[DECISÃO]**
- Autenticação via **JWT** (access token de curta duração, ex.: 15 min) + **refresh token** (maior duração, ex.: 7 dias), este último armazenado em cookie `httpOnly`, `Secure` (em produção), `SameSite=Lax` para mitigar XSS/CSRF.
- Autorização por papel e por posse (ownership) usando `@PreAuthorize` do Spring Security nos controllers/serviços, nunca apenas checagem no frontend.
- Front-end nunca guarda o access token em `localStorage` (mitigação de XSS); mantém em memória/estado do app, renovando via endpoint de refresh.

**Implementado na Fase 1 (detalhe relevante para fases futuras):** como `apps/web` (ex.: `localhost:3000`) e `apps/api` (ex.: `localhost:8080`) são origens diferentes, o cookie `httpOnly` de refresh token pertence ao domínio da API e **não é legível pelo servidor Next.js** (middleware/SSR) — apenas o navegador o envia automaticamente em chamadas `fetch` com `credentials: "include"` diretamente à API. Por isso, a proteção de rota no frontend é feita **no client-side**:
- Um `AuthProvider` (`apps/web/src/lib/auth-context.tsx`) chama `POST /auth/refresh` ao montar a aplicação para tentar obter um access token novo a partir do cookie; em caso de sucesso, busca `GET /auth/me` para popular o usuário/papéis em memória.
- Um componente `ProtectedRoute` (`apps/web/src/components/protected-route.tsx`) redireciona para `/login` se não autenticado, ou para `/dashboard` se o papel não é permitido, exibindo um skeleton de carregamento enquanto isso.
- Toda regra de autorização real permanece no backend (`@PreAuthorize`); a proteção de rota no frontend é apenas UX, nunca a única barreira.
- Caso um domínio compartilhado (ex.: `app.infoprodutos.com` + `api.infoprodutos.com` com cookie em `.infoprodutos.com`) seja adotado em produção, um middleware Next.js baseado em cookie passa a ser possível como camada adicional — não implementado na Fase 1 por não ser o cenário de desenvolvimento local.

## 9. Requisitos não funcionais (MVP)

| Categoria | Requisito |
|---|---|
| Disponibilidade | Ambiente único de produção no MVP, sem HA multi-região (fora de escopo) |
| Observabilidade | Logs estruturados; auditoria de ações administrativas (`AuditLog`); **[PERGUNTA ABERTA]** ferramenta de APM/observabilidade externa não definida |
| Internacionalização | `[SUPOSIÇÃO]` MVP em pt-BR apenas; campo `language` já modelado no pipeline de IA para permitir expansão futura |
| Acessibilidade | Componentes baseados em Radix (shadcn/ui) para suporte a teclado/leitor de tela por padrão |
| Performance | Sem metas numéricas definidas ainda — **[PERGUNTA ABERTA]** |
| Escalabilidade | Arquitetura modular permite extrair o subsistema de IA como serviço próprio no futuro, sem reescrever o domínio |

## 10. Ambiente local (infra)

`infra/docker-compose.yml` (a ser criado na Fase 0) deve subir:
- PostgreSQL 16 com volume persistente e usuário/senha via variáveis de ambiente (nunca hardcoded).
- `apps/api` (perfil `dev`) apontando para o Postgres local e para a implementação local de storage de vídeo.
- `apps/web` apontando para a API local.

Segredos (chaves de provedor de IA, credenciais de storage) nunca entram no `docker-compose.yml` versionado — apenas referências a variáveis de ambiente, com `.env.example` documentando as chaves esperadas sem valores reais (ver `SECURITY.md`).

## 11. Diagrama de deployment (alvo de produção, conceitual)

```mermaid
graph LR
  Users((Usuários)) --> CDN["CDN / Edge<br/>(assets estáticos do Next.js)"]
  Users --> WebProd["apps/web<br/>(Next.js, SSR/edge)"]
  WebProd --> ApiProd["apps/api<br/>(Spring Boot)"]
  ApiProd --> PgProd[("PostgreSQL gerenciado")]
  ApiProd --> ObjStorage["Object Storage S3-compatible"]
  ApiProd --> AIProviders["Provedores de IA<br/>(transcrição / geração)"]
```

**[PERGUNTA ABERTA]** Provedor de hospedagem para produção (AWS, GCP, Azure, Railway, Fly.io, VPS próprio) ainda não definido. Este documento não assume um provedor específico; a arquitetura é agnóstica de nuvem por design (uso de S3-compatible, Postgres padrão, containers).

## 12. Documentos relacionados

Ver `DATABASE.md`, `API.md`, `AI_PIPELINE.md`, `SECURITY.md`.
