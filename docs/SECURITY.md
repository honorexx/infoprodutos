# SECURITY — Modelo de Ameaças e Mitigações

Status: **Rascunho para aprovação**
Versão: 0.1.0

## 1. Princípios

1. Autorização sempre no backend. Ocultar UI não é controle de acesso.
2. Toda entrada do usuário (incluindo conteúdo de aula transcrito) é não confiável.
3. Segredos nunca em código-fonte, nunca versionados.
4. Falha segura: em caso de erro, negar acesso e não vazar detalhes internos.
5. Auditoria de toda ação administrativa relevante.

## 2. Autenticação

- Senhas: hash com **bcrypt** (ou Argon2) via Spring Security, custo configurável; nunca texto plano, nunca reversível.
- **[DECISÃO]** Access token JWT (curta duração, ex.: 15 min), assinado com chave (HS256 com segredo forte via variável de ambiente, ou RS256 com par de chaves — **[PERGUNTA ABERTA]**: confirmar preferência; proposta padrão HS256 para simplicidade do MVP single-service).
- Refresh token de maior duração (ex.: 7 dias), armazenado em cookie `httpOnly`, `Secure`, `SameSite=Lax`, nunca acessível via JavaScript no frontend (mitiga roubo por XSS).
- Rotação de refresh token a cada uso (mitiga replay em caso de vazamento).
- Recuperação de senha via token de uso único e curta expiração, enviado por e-mail; token nunca retornado na resposta da API (somente enviado por canal separado).
- Registro de `last_login_at` em cada autenticação bem-sucedida.
- Bloqueio lógico de conta (`status = BLOCKED`) impede login e qualquer ação, mas preserva 100% do histórico acadêmico/auditoria (nunca exclusão física de `User`).

## 3. Autorização (RBAC + ownership)

- Papéis: `SUPER_ADMIN`, `INSTRUCTOR`, `STUDENT` (extensível via tabela `role`, não hardcoded como enum de banco).
- **[DECISÃO]** Autorização em duas camadas:
  1. **Papel** — `@PreAuthorize("hasRole('INSTRUCTOR')")` nos controllers/serviços.
  2. **Posse (ownership)** — checagem explícita no serviço (ex.: "este `Course.created_by_user_id` ou vínculo em `CourseInstructor` corresponde ao usuário autenticado?") antes de qualquer leitura/escrita de recurso específico. Um `INSTRUCTOR` autenticado não pode editar curso de outro apenas por possuir o papel.
- STUDENT nunca tem endpoint de escrita em conteúdo (curso/módulo/aula/questão) habilitado no backend — não é apenas ausência de botão no frontend.
- Acesso a vídeo e material exige `Enrollment.status = ACTIVE` (ou `Lesson.access_type = FREE_PREVIEW`) verificado a cada emissão de URL assinada — nunca cache de autorização de longa duração.

## 4. Segredos e configuração

- **[DECISÃO]** Todas as credenciais (banco, chave de assinatura JWT, chave de provedor de IA, credenciais de storage) via variáveis de ambiente, lidas pelo Spring (`application.yml` com `${VAR}`) e pelo Next.js (`.env.local`, nunca commitado).
- `.env.example` na raiz de `apps/api` e `apps/web` documentando as chaves esperadas **sem valores reais**.
- `.gitignore` cobre `.env`, `.env.local`, `.env*.local` e quaisquer arquivos de credenciais.
- Nenhuma chamada a provedor de IA parte do frontend — sempre via backend, chave nunca exposta ao navegador (ver `AI_PIPELINE.md`).
- **[PERGUNTA ABERTA]** Estratégia de gestão de segredos em produção (variáveis de ambiente do provedor de hospedagem vs. um secrets manager dedicado) depende da decisão de hospedagem, ainda em aberto (`ARCHITECTURE.md` §11).

## 5. Proteção de conteúdo (vídeos e materiais)

- Vídeos nunca servidos por URL pública permanente. Implementação local (dev) exige token de sessão válido no endpoint de streaming; implementação de produção usa URLs assinadas de curta expiração (ex.: 5–15 min), geradas somente após validação de matrícula ativa.
- Materiais complementares seguem a mesma checagem de matrícula antes de gerar link de download.
- Validação de tipo e tamanho de arquivo no upload (vídeo e materiais): allowlist de `mime_type` (ex.: `video/mp4`, `application/pdf`, etc. — **[PERGUNTA ABERTA]** lista exata de formatos aceitos a confirmar) e limite máximo de tamanho configurável por variável de ambiente.
- **[DECISÃO]** Verificação do `mime_type` real do arquivo (magic bytes), não apenas a extensão ou o `Content-Type` declarado pelo cliente, para mitigar upload de arquivo malicioso disfarçado.

## 6. Tratamento de erros e observabilidade

- Respostas de erro para o cliente seguem o formato padronizado (`API.md` §1), sem stack trace, nomes de classe interna, ou informações de schema do banco.
- Logs internos estruturados incluem `correlationId` (retornado ao cliente em erros 5xx) para permitir suporte sem expor detalhes ao usuário final.
- `AuditLog` registra, no mínimo: publicação/despublicação/arquivamento de curso, bloqueio/desbloqueio de usuário, concessão/suspensão/cancelamento de matrícula, aprovação/rejeição de questão gerada por IA, emissão e revogação de certificado.

## 7. Segurança específica da IA

### 7.1 Prompt injection

O conteúdo de uma aula (transcrição) é conteúdo gerado por terceiros (o professor, ou a própria fala capturada no vídeo) e é tratado como **dado, nunca como instrução**. Mitigações:

- Separação estrita entre mensagem de sistema (regras fixas de geração) e conteúdo do usuário (transcrição + instruções adicionais do professor) na chamada ao provedor de IA — nunca concatenação livre em uma única string sem delimitação clara.
- `AiContentValidator` não confia apenas na obediência do modelo: revalida estruturalmente cada questão (schema, evidência, contagem de alternativas) independentemente do que o modelo "disse" ter feito.
- Instruções adicionais do professor (`additionalInstructions`) são tratadas como sugestão de estilo/foco, nunca como comando capaz de alterar o formato de saída ou desabilitar validações — a validação estrutural do backend é sempre aplicada, independentemente do conteúdo do prompt.
- Nenhuma aula pode fazer a IA "vazar" instruções de sistema ou dados de outros cursos: cada chamada é isolada por `lessonId`/`transcriptId`, sem contexto compartilhado entre gerações de aulas diferentes.

### 7.2 Minimização de dados pessoais

Somente título da aula, transcrição e instruções do professor são enviados ao provedor de IA (ver `AI_PIPELINE.md` §14). Nenhum dado de aluno (nome, e-mail, notas, progresso) é enviado a provedores externos de IA em nenhuma circunstância do MVP.

### 7.3 Controle de custo e abuso

- Rate limiting no endpoint de solicitação de geração (`API.md` §3).
- `idempotencyKey` obrigatória evita geração duplicada por duplo clique/retry.
- Registro de uso (`AiUsageTracker`) permite auditoria de custo por curso/professor mesmo sem limites automáticos no MVP.

## 8. Dados falsos em produção

**[DECISÃO — requisito explícito do usuário]** Nenhum seed de dados fictícios (usuários de teste, cursos de exemplo, matrículas fake) é executado em ambiente de produção. Scripts de seed ficam isolados sob um perfil Spring `dev`/`local` e sob proteção explícita (ex.: falha ao subir se `spring.profiles.active=prod` e o seed for invocado). Ambiente de produção começa vazio, populado apenas por ações reais de administradores/professores.

## 9. Validação pública de certificado

- Endpoint público (`GET /public/certificates/validate/{validationCode}`) retorna **apenas**: nome do aluno, nome do curso, carga horária, data de conclusão/emissão e status (válido/revogado). **Não** inclui nome do instrutor (certificado institucional).
- Não retorna: e-mail, identificadores internos (UUID), dados de outras matrículas, progresso ou qualquer dado não estritamente necessário à validação.
- `validation_code` é um código curto dedicado (não o UUID do certificado), reduzindo a chance de enumeração e evitando reaproveitar um identificador interno em contexto público.
- **[DECISÃO]** Rate limiting leve neste endpoint também é recomendável para evitar varredura de códigos válidos por força bruta — **[PERGUNTA ABERTA]** definir limite exato.

## 10. Matriz de riscos e mitigações (resumo)

| Risco | Impacto | Mitigação |
|---|---|---|
| Aluno acessa vídeo sem matrícula ativa | Alto | URL assinada de curta duração, checagem de `Enrollment.status` a cada emissão |
| Aluno altera progresso de outro aluno | Alto | Checagem de posse (`enrollment.student_user_id == usuário autenticado`) em toda escrita |
| Vazamento de chave de provedor de IA | Alto | Chamadas de IA só no backend; chave via variável de ambiente; nunca logada |
| Prompt injection via conteúdo de aula | Médio-Alto | Delimitação de prompt + validação estrutural independente do modelo |
| Publicação automática de questão de IA incorreta | Alto | Regra inegociável de aprovação humana antes de `PUBLISHED` |
| Upload de arquivo malicioso disfarçado | Médio | Validação de `mime_type` real (magic bytes) + limite de tamanho |
| Enumeração de e-mails via `/auth/register` ou `/auth/login` | Baixo-Médio | Respostas genéricas + rate limiting |
| Exposição de stack trace/erro interno | Médio | Formato de erro padronizado, sem detalhes internos, `correlationId` para suporte |
| Duplicação de questões por retry de geração | Médio | `idempotencyKey` obrigatória em `AiGenerationJob` |
| Dados fictícios vazando para produção | Médio | Seeds restritos a perfis `dev`/`local`, bloqueio explícito em `prod` |

## 11. Documentos relacionados

`ARCHITECTURE.md` §8 (autenticação/sessão), `AI_PIPELINE.md` §14 (dados enviados à IA), `API.md` §1 e §3 (erros e rate limiting), `TEST_STRATEGY.md` (casos de teste de segurança).
