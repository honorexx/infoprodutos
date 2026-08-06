# PRD — Plataforma de Cursos Online e Infoprodutos

Status: **Rascunho para aprovação** — aguardando "ARQUITETURA APROVADA".
Versão: 0.1.0
Última atualização: 2026-08-06

> Convenções usadas neste documento e nos demais em `/docs`:
> - **[DECISÃO]** — algo já definido pelo time técnico, com justificativa registrada em `DECISIONS.md`.
> - **[SUPOSIÇÃO]** — premissa assumida na ausência de informação, que precisa ser confirmada por Pedro.
> - **[PERGUNTA ABERTA]** — decisão de produto/negócio que depende de resposta explícita antes da implementação da fase correspondente.

## 1. Visão do produto

Construir uma plataforma própria (não um SaaS de terceiros) de cursos online e infoprodutos, conceitualmente um LMS enxuto, com três superfícies:

1. **Painel administrativo** — operação da plataforma (SUPER_ADMIN).
2. **Área do professor** — criação e gestão de cursos, incluindo revisão de exercícios gerados por IA (INSTRUCTOR).
3. **Área do aluno** — consumo de cursos, progresso e certificação (STUDENT).

O diferencial de produto do MVP é o **pipeline de IA para geração assistida de exercícios** a partir da transcrição das aulas, sempre com revisão humana obrigatória antes da publicação — não é um chatbot genérico.

## 2. Objetivos do MVP

- Permitir que um professor estruture um curso (módulos → aulas → vídeo + materiais) e o publique.
- Permitir que um aluno com matrícula ativa assista às aulas, acompanhe o progresso e responda exercícios.
- Emitir certificado automaticamente quando os critérios de conclusão do curso forem atingidos, com validação pública por código.
- Acelerar a criação de avaliações usando IA sobre o conteúdo real da aula, mantendo controle humano total sobre o que é publicado.
- Garantir que toda autorização de acesso (conteúdo, vídeo, ações administrativas) seja aplicada no backend.

## 3. Não objetivos do MVP

Ver lista completa e justificativas em `ROADMAP.md` (seção "Fora do escopo"). Resumo: sem app mobile, sem rede social/chat/lives, sem marketplace de professores, sem afiliados, sem gamificação complexa, sem pagamento/assinatura automática, sem integração com Ads, sem tutor de IA conversacional, sem fine-tuning/treinamento de modelo próprio, sem publicação automática de questões de IA.

## 4. Personas

| Persona | Papel | Necessidade principal |
|---|---|---|
| Administrador da plataforma | SUPER_ADMIN | Visão global, gestão de usuários e conteúdo, métricas |
| Professor / criador de conteúdo | INSTRUCTOR | Criar cursos rapidamente e gerar exercícios sem trabalho manual repetitivo |
| Aluno | STUDENT | Assistir aulas, acompanhar progresso, obter certificado confiável |
| Visitante (não autenticado) | — | Validar um certificado publicamente |

## 5. Perfis e permissões (resumo funcional)

Especificação completa de regras de autorização está em `SECURITY.md`. Resumo:

### SUPER_ADMIN
- Gerencia toda a plataforma, usuários e cursos de qualquer professor.
- Visualiza métricas globais.
- Pode publicar, editar ou remover qualquer conteúdo.

### INSTRUCTOR
- Cria/edita apenas os próprios cursos (ou cursos onde consta como `CourseInstructor`).
- Cria módulos, aulas; envia vídeos e materiais.
- Solicita geração de exercícios por IA; revisa, edita, aprova, rejeita questões.
- Acompanha progresso dos alunos apenas dos seus cursos.

### STUDENT
- Acessa somente cursos com matrícula ativa (`Enrollment.status = ACTIVE`).
- Assiste aulas, marca/concluí aulas, responde exercícios, acompanha o próprio progresso.
- Emite certificado quando elegível.
- Não cria, edita ou exclui conteúdo — **[DECISÃO]**: toda regra é reforçada no backend via `@PreAuthorize`/checagem de posse (ownership), nunca apenas ocultando botões no frontend.

**[SUPOSIÇÃO]** Um usuário pode acumular mais de um papel (ex.: um INSTRUCTOR também pode ser STUDENT em cursos de terceiros). Modelamos papéis como relação N:N (`UserRole`) em vez de campo único, para suportar isso sem migração futura. Confirmar se este comportamento é desejado.

## 6. Escopo funcional do MVP

Ver descrição funcional completa na mensagem original do usuário (seções 3.1 a 3.10). Este PRD referencia os módulos funcionais; o detalhamento técnico (campos, regras, contratos) está em `DATABASE.md`, `API.md` e `AI_PIPELINE.md`. Lista de módulos:

1. Autenticação e gestão de conta.
2. Painel administrativo (dashboard, CRUD de cursos, publicação).
3. Módulos (estrutura curricular).
4. Aulas (conteúdo, vídeo, materiais).
5. Vídeos (abstração de armazenamento, sem blobs no banco).
6. Matrículas (concessão manual no MVP, sem pagamento).
7. Progresso (aula e curso).
8. Exercícios (múltipla escolha, manual ou gerado por IA).
9. Tentativas e respostas (correção determinística no backend).
10. Certificado (emissão + validação pública).
11. Subsistema de IA (transcrição → geração → revisão humana).

## 7. Critérios de conclusão de aula — regra proposta

**[PERGUNTA ABERTA]** O enunciado pede "regras claras para considerar uma aula concluída", mas não define o gatilho exato. Proposta padrão a confirmar:

- Vídeo: aula é marcada `COMPLETED` quando o aluno atinge **≥ 90% da duração do vídeo assistida** (posição reportada pelo player) OU o aluno clica manualmente em "Marcar como concluída".
- Aula sem vídeo (ex.: apenas material): concluída apenas por ação manual do aluno.
- Regressão: se o aluno voltar o vídeo, o status não regride de `COMPLETED` para `IN_PROGRESS` (o progresso é monotônico), mas `last_position_seconds` continua sendo atualizado para retomada.

Esta regra deve ser confirmada antes da Fase de Progresso (ver `ROADMAP.md`).

## 8. Critérios de emissão de certificado — regra proposta

**[PERGUNTA ABERTA]** Confirmar os três limiares configuráveis por curso:

- `min_completion_percentage` (% mínimo de aulas concluídas, padrão sugerido: 100%).
- Realização de todos os exercícios **obrigatórios** do curso (todo `Quiz` vinculado a um módulo publicado é obrigatório, salvo indicação contrária — a definir).
- `min_passing_score` (nota mínima média entre as tentativas válidas, padrão sugerido: 70%).

**[SUPOSIÇÃO]** Nota final por quiz = melhor tentativa (`MAX(score)`) dentre as tentativas permitidas, não a média das tentativas. A confirmar.

## 9. Métricas de sucesso do MVP (sugeridas)

**[PERGUNTA ABERTA]** — nenhuma meta numérica foi fornecida. Sugestão de métricas a acompanhar (sem metas fixadas ainda):
- Tempo médio entre "vídeo enviado" e "questões disponíveis para revisão".
- % de questões geradas por IA aprovadas sem edição vs. editadas vs. rejeitadas.
- Taxa de conclusão de curso por matrícula.
- Taxa de emissão de certificado sobre matrículas elegíveis.

## 10. Restrições e premissas gerais

- Ambiente de produção não deve conter dados fictícios (seeds de demonstração ficam restritos a ambiente local/dev — ver `SECURITY.md` e `infra/`).
- Pagamentos estão fora do MVP; toda concessão de acesso é manual (`Enrollment` criado por SUPER_ADMIN ou INSTRUCTOR autorizado).
- A plataforma é multi-tenant lógica única (uma instalação = uma organização). **[SUPOSIÇÃO]** Multi-tenancy (múltiplas escolas/organizações isoladas) não está no MVP; não implementado, mas o modelo de dados evita acoplamentos que impeçam evolução futura.

## 11. Glossário

| Termo | Significado |
|---|---|
| LMS | Learning Management System |
| Matrícula (Enrollment) | Vínculo ativo entre aluno e curso que autoriza acesso |
| Job de IA | Execução assíncrona do pipeline de transcrição/geração |
| Evidência | Trecho da transcrição que fundamenta uma questão gerada por IA |
| Rascunho (DRAFT) | Estado de conteúdo não publicado/visível a alunos |

## 12. Documentos relacionados

- `ARCHITECTURE.md` — arquitetura técnica e stack.
- `DATABASE.md` — modelo de dados completo.
- `API.md` — contratos de API.
- `AI_PIPELINE.md` — pipeline de IA em detalhe.
- `SECURITY.md` — modelo de ameaças e mitigações.
- `ROADMAP.md` — fases de entrega e critérios de aceite.
- `TEST_STRATEGY.md` — estratégia de testes.
- `DECISIONS.md` — registro de decisões, suposições e perguntas abertas.
