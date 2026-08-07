# DESIGN_SYSTEM — Sistema Visual "Infoprodutos"

Status: **Implementado (frontend)** — cobre a área pública, autenticação e o painel
administrativo/professor entregues até a Fase 2. Não altera regras de negócio,
contratos de API, banco de dados ou funcionalidades — é exclusivamente a camada
visual, de experiência e de animação do frontend (`apps/web`).

Este documento é a fonte da verdade para decisões visuais. Qualquer novo
componente ou tela deve ser construído a partir dos tokens e padrões descritos
aqui, não com valores arbitrários soltos no JSX.

## 1. Direção criativa

**Posicionamento:** educação digital premium com linguagem editorial.

A plataforma deve parecer desenhada por uma pessoa para um produto sério de
educação — não montada a partir de um template de dashboard genérico, não uma
cópia de Alura/Hotmart/Udemy, e não "mais uma interface roxa/azul de startup de
IA". Isso se traduz em decisões concretas:

- **Tinta em vez de gradiente.** A cor primária é uma tinta quase preta (não
  puro preto), usada com confiança em botões e texto. O verde de destaque é
  reservado para o que realmente merece atenção (links, estado ativo, tags,
  foco), nunca para preencher grandes áreas.
- **Tipografia com personalidade.** Um serifado editorial (Fraunces) para
  títulos, combinado com uma sans humanista (Public Sans) para interface —
  evita a combinação "Inter em tudo" que qualquer gerador de UI produz por
  padrão.
- **Estrutura em vez de decoração.** Hierarquia construída com tipografia,
  espaçamento e divisores — não com sombra pesada, glassmorphism ou brilho.
- **Honestidade de conteúdo.** Nenhum número, depoimento ou métrica é
  inventado. Onde uma funcionalidade ainda não existe (vídeo, IA, matrícula,
  certificados), a interface diz isso claramente ("Em breve", "Disponível na
  próxima fase") em vez de simular dados falsos.

### O que evitar (lista de verificação)

| Não fazer | Fazer no lugar |
|---|---|
| Gradiente roxo-azulado cobrindo hero/CTA | Fundo sólido off-white/carvão + um único acento verde |
| Cartão para cada informação pequena | Linhas com tipografia + divisores (`divide-y`) |
| Quatro métricas idênticas em grade | Uma métrica primária grande + métricas compactas variadas |
| Botões/menus permanentes em toda linha de tabela | Ações primárias visíveis (mover) + menu "···" para o resto |
| Depoimentos/números inventados | Placeholder explícito ("Espaço reservado", "Exemplo") |
| Ícones em `rounded-full` roxo brilhante | Selos com cor semântica e cantos discretos (`rounded-md`) |
| Texto todo em negrito para hierarquia | Tamanho + peso + cor + tracking para hierarquia |

## 2. Marca

Marca: **Infoprodutos**. Símbolo: três hastes verticais ascendentes (progresso
profissional), a mais alta destacada em verde — `src/components/logo.tsx`
(`LogoMark` para o ícone isolado, `Logo` para ícone + nome).

Uso:
- Fundo claro (área pública): marca escura sobre off-white.
- Sidebar (sempre em tinta escura, mesmo no tema claro — ver §3): marca em
  verde sobre o fundo `--sidebar`.
- Nunca recolorir o símbolo fora dos tokens `foreground`/`accent`/`sidebar-*`.

## 3. Paleta de cores

Todos os tokens vivem em `apps/web/src/app/globals.css`, como variáveis CSS em
`:root` (claro) e `.dark` (escuro), expostas ao Tailwind via `@theme inline`.
**Nunca** hard-code uma cor em um componente — use as classes utilitárias
(`bg-accent`, `text-muted-foreground`, etc.) que resolvem para esses tokens.

### 3.1 Papéis semânticos

| Token | Papel | Uso típico |
|---|---|---|
| `background` / `foreground` | Fundo e texto principal | Página, corpo de texto |
| `card` / `card-foreground` | Superfície elevada discreta | Linha de lista, painel |
| `muted` / `muted-foreground` | Fundo neutro / texto secundário | Fundos de seção, legendas |
| `primary` / `primary-foreground` | Ação principal (tinta) | Botão padrão, texto de ênfase |
| `secondary` / `secondary-foreground` | Ação secundária neutra | Botão secundário |
| `accent` / `accent-foreground` | Destaque de marca (verde) | Links, estado ativo, ícones-chave, foco |
| `gold` / `gold-foreground` | Complementar (âmbar) | Pequenos detalhes, alertas neutros de atenção |
| `success` / `warning` / `info` (+ `-foreground`) | Estados semânticos | Badges de status, alertas |
| `destructive` / `destructive-foreground` | Ação/estado destrutivo | Excluir, erro |
| `border` / `input` / `ring` | Bordas, campos, foco | — |
| `sidebar*` | Família de cores da navegação (sempre escura) | `AppShell` |

### 3.2 Valores (OKLCH)

**Claro**
```
--background: oklch(0.975 0.006 85)   /* off-white quente */
--foreground: oklch(0.19 0.012 75)    /* quase preto, quente */
--primary:    oklch(0.19 0.012 75)    /* tinta */
--accent:     oklch(0.46 0.11 152)    /* verde sofisticado */
--gold:       oklch(0.77 0.13 83)     /* âmbar suave */
--destructive:oklch(0.53 0.19 25)
```

**Escuro**
```
--background: oklch(0.18 0.01 75)     /* carvão profundo, não preto puro */
--foreground: oklch(0.94 0.008 85)
--primary:    oklch(0.94 0.008 85)    /* inverte: quase branco em botões */
--accent:     oklch(0.68 0.14 152)    /* verde mais claro para contraste */
```

Motivo de usar OKLCH: interpolação de cor perceptualmente uniforme (os
`color-mix(in oklch, ...)` usados em estados de hover não "sujam" a cor) e
controle direto de luminosidade para acessibilidade.

### 3.3 Regras de uso

- **Verde com moderação**: no máximo um elemento de destaque por bloco visual
  (um ícone, um link, um badge). Nunca pintar uma seção inteira de verde.
- **Primário = tinta**, não verde. Botões de ação principal usam `variant="default"`
  (tinta). Use `variant="accent"` apenas para uma CTA secundária que precise
  competir visualmente com a primária (raro).
- **Semânticas não se misturam**: sucesso é sempre verde (`success`), atenção
  é sempre âmbar (`warning`), nunca o inverso.
- **Contraste**: todo par cor de fundo/texto usado nos tokens acima atende
  WCAG AA para texto normal (verificado nas combinações padrão dos
  componentes; qualquer combinação nova deve ser conferida antes do uso).

## 4. Tipografia

| Papel | Fonte | Variável CSS | Uso |
|---|---|---|---|
| Heading | Fraunces (serifado editorial, variável) | `--font-heading` | `h1`–`h4`, títulos de card/dialog, kickers de marca |
| Interface | Public Sans (humanista) | `--font-sans` | Corpo de texto, formulários, tabelas |
| Monoespaçada | Geist Mono | `--font-mono` | Códigos (ex.: correlationId, futuros códigos de certificado) |

Carregadas via `next/font/google` em `src/app/layout.tsx`, sem chamadas de
rede em runtime (self-hosted pelo Next).

### 4.1 Escala e hierarquia

Não existe uma escala numérica nova — usamos a escala do Tailwind, mas com
regras de uso fixas:

| Nível | Classe típica | Onde |
|---|---|---|
| Título de página (H1) | `font-heading text-2xl sm:text-3xl font-medium tracking-tight` | Topo de cada tela do painel |
| Título hero (H1 público) | `font-heading text-4xl sm:text-5xl lg:text-[3.4rem] font-medium` | Landing |
| Título de seção (H2) | `font-heading text-lg` a `text-3xl` conforme contexto, `font-medium tracking-tight` | Seções da landing, "Estrutura curricular" |
| Título de curso/card | `font-heading text-base` a `text-xl`, `font-medium` | `CardTitle`, linha da listagem de cursos |
| Nome de módulo/aula | `text-sm font-medium` (sans, não heading) | Itens de lista dentro do construtor |
| Texto de interface | `text-sm` (sans) | Corpo padrão, formulários |
| Informação secundária | `text-sm text-muted-foreground` | Descrições, metadados |
| Métricas | `font-heading text-2xl` a `text-5xl font-medium` | Números do dashboard |
| Legenda / rótulo | `text-xs text-muted-foreground` | Timestamps, contadores |
| Kicker (rótulo de seção) | `.kicker` (ver §7) | Acima de títulos de seção |

Regras:
- **Nunca** usar heading gigante "porque sim" — o tamanho reflete a
  importância real da informação (uma métrica de dashboard pode ser maior que
  o H1 da própria página).
- Hierarquia é construída combinando tamanho + peso (`font-medium`, raramente
  `font-semibold`/`font-bold`) + `tracking-tight` em headings + cor
  (`text-muted-foreground` para secundário). Evite excesso de negrito.
- Texto longo de leitura usa `leading-relaxed`; títulos usam `leading-tight`/`leading-snug`.

## 5. Espaçamento, raio e sombra

- **Espaçamento**: escala padrão do Tailwind (`gap-2`, `p-6`, `p-8`...). Telas
  internas do painel usam contêiner `mx-auto max-w-4xl`/`max-w-5xl` com
  `p-6 sm:p-8`. Seções da landing usam `py-20`.
- **Raio** (`--radius: 0.5rem` como base, com escala derivada em
  `--radius-sm/md/lg/xl/2xl`): controles de formulário e botões usam
  `rounded-md`; cartões e diálogos usam `rounded-lg`; badges usam um raio
  pequeno (`rounded-[4px]`) — deliberadamente **não** pill/cápsula, para um ar
  mais editorial ("etiqueta impressa") do que "chip de app".
- **Sombra**: duas utilidades apenas — `shadow-soft` (botões primários,
  cartões elevados) e `shadow-lifted` (menus flutuantes, diálogos). Nunca
  sombra difusa grande ou glow colorido.

## 6. Ícones

Biblioteca: `lucide-react`, tamanho `size-4` por padrão dentro de botões/itens
de navegação, `size-3.5` em contextos compactos (linhas de tabela, ações
secundárias). Ícones são sempre `aria-hidden` quando acompanhados de texto
visível; quando isolados (botão de ícone), o componente precisa de
`sr-only`/`aria-label`.

## 7. Utilitários de marca

Definidos em `globals.css`:

- `.kicker` — rótulo de seção: traço curto + texto verde, uppercase,
  tracking largo. Usado antes de títulos de seção na landing, no dashboard e
  em telas de autenticação.
- `.shadow-soft` / `.shadow-lifted` — ver §5.
- `::selection` — seleção de texto usa o par `accent`/`accent-foreground`.
- `@keyframes shimmer` — usado pelo `Skeleton` para um efeito de
  carregamento mais sofisticado que um pulso genérico.

## 8. Componentes (base shadcn/ui personalizada)

Todos os componentes abaixo vivem em `src/components/ui/` e usam **apenas**
os tokens acima. `shadcn/ui` é a base técnica (Radix + CVA); a aparência
padrão da biblioteca foi sobrescrita.

| Componente | Personalização aplicada |
|---|---|
| `Button` | Variante `default` = tinta com `shadow-soft`; nova variante `accent` (verde, uso raro); hovers via `color-mix` em vez de opacidade genérica; raio `rounded-md`. |
| `Badge` | Uppercase, tracking largo, raio pequeno (não pill); variantes semânticas novas: `accent`, `gold`, `success`, `warning`, `info`. |
| `StatusBadge` | Mapeia `DRAFT`→secundário, `PUBLISHED`→`success`, `ARCHIVED`→`destructive`. |
| `Input` / `Textarea` / `Select` | Fundo sólido (`bg-background`), `rounded-md`, anel de foco no acento com opacidade reduzida (25%) para um foco mais discreto. |
| `Dialog` | `rounded-lg`, `shadow-lifted`, título em `font-heading text-lg`. |
| `Card` | Borda fina em vez de `ring`; `rounded-lg`; usado com moderação (ver §1). |
| `Alert` | Variantes `success`/`warning`/`info` adicionadas, com fundo tingido sutil (5–10% de opacidade). |
| `Skeleton` | Efeito shimmer (gradiente animado) em vez de pulso plano. |
| `Table` | Cabeçalho uppercase compacto (`text-xs tracking-wide text-muted-foreground`), linhas com hover sutil. |
| `DropdownMenu` | `shadow-lifted`, `rounded-md`, usado como menu de ações contextual ("···") no construtor de cursos. |
| `Tooltip` (novo) | Fundo `foreground`/texto `background` (alto contraste), usado nos rótulos da sidebar recolhida. |
| `Progress` (novo) | Barra fina (`h-1.5`), indicador em `accent`, usada no dashboard (proporção de cursos publicados). |
| `Avatar` | Iniciais do usuário sobre `sidebar-accent`. |

### 8.1 Estados obrigatórios

Cada componente interativo cobre, no mínimo:

- **Normal / Hover / Focus** — `focus-visible:ring-3 focus-visible:ring-ring/25|50`
  em todos os controles de formulário e botões; nunca remover o anel de foco.
- **Active** — leve translação (`active:translate-y-px`) em botões, para
  feedback tátil sem exagero.
- **Disabled** — `disabled:opacity-50 disabled:pointer-events-none`.
- **Loading** — botões de submit trocam o rótulo ("Entrando...", "Criando...")
  e ficam `disabled`; listas usam `Skeleton`.
- **Success / Warning / Error** — `Alert`/`Badge` com as variantes semânticas
  correspondentes; toasts (`sonner`, `src/components/ui/sonner.tsx`) usam os
  mesmos ícones semânticos (`CircleCheckIcon`, `TriangleAlertIcon`, etc.).
- **Empty state** — nunca uma tela em branco: bordas tracejadas
  (`border-dashed`) + texto explicativo + CTA quando aplicável (ex.: "Você
  ainda não tem nenhum curso.", "Nenhum módulo criado ainda.").

## 9. Estrutura das três experiências

### 9.1 Área pública (`/`, `/login`, `/register`)

- `SiteHeader` só renderiza fora de sessão autenticada.
- Landing (`src/app/page.tsx`): hero assimétrico (texto + prévia ilustrativa
  da interface, claramente rotulada "Exemplo"), seção de benefícios em lista
  numerada (não três ícones idênticos), split "para professores"/"para
  administradores", seção de depoimentos **honesta** (sem inventar citações),
  FAQ em acordeão nativo (`<details>`), footer minimalista.
- Login/Registro: layout dividido (`AuthSplitLayout` —
  `src/components/auth-split-layout.tsx`) — painel de marca à esquerda
  (oculto em telas pequenas) + formulário à direita. Não é mais um card
  centralizado genérico.

### 9.2 Painel administrativo / do professor

- `AppShell` (`src/components/app-shell.tsx`): sidebar fixa em desktop,
  recolhível para modo ícone (preferência persistida em
  `localStorage["infoprodutos:sidebar-collapsed"]`), com tooltip por item
  quando recolhida; menu overlay em mobile.
- Navegação dividida em **Principal** (rotas reais: Visão geral, Cursos,
  Usuários) e **Próximas fases** (Alunos, Processamentos de IA, Certificados,
  Configurações) — itens futuros aparecem desabilitados com selo "Em breve"
  em vez de links quebrados ou telas falsas. Isso respeita a regra do projeto
  de não inventar funcionalidade ainda não implementada.
- Dashboard (`src/app/dashboard/page.tsx`): uma métrica primária (total de
  cursos, com barra de progresso de publicação) + métricas compactas
  (publicados/rascunhos/arquivados) + "Precisam de atenção" (cursos em
  rascunho) + "Atividade recente" (por `updatedAt`) + bloco explícito
  "Disponível nas próximas fases" para métricas que ainda não existem (alunos
  ativos, IA pendente, taxa de conclusão). Tudo calculado a partir de dados
  reais da API (`/courses`, `/users`), nunca inventado.

### 9.3 Construtor de cursos

`src/app/courses/[id]/page.tsx`:

- Cabeçalho do curso com metadados (slug, autor, carga horária, contagem de
  módulos/aulas) e ações de ciclo de vida (Publicar/Despublicar/Arquivar).
- Árvore curricular com módulos **expansíveis/recolhíveis** (estado local,
  ícone de chevron rotativo).
- Cada linha de módulo/aula mostra apenas as ações mais usadas de forma
  permanente (mover para cima/baixo) e agrupa o resto (publicar, excluir) em
  um menu contextual "···" (`DropdownMenu`) — evita poluição visual de vários
  botões fixos por linha.
- Aula sem duração exibe "sem vídeo" (upload de vídeo é Fase 3) em vez de
  aparentar um campo vazio sem explicação; um aviso único no topo da seção
  informa que vídeos e exercícios chegam nas Fases 3 e 5.
- Aviso de atenção (`Info` + texto) é único por curso, não repetido em cada
  linha, para não virar ruído.

## 10. Responsividade

- Abordagem *desktop-first* em componentes de navegação (sidebar) com
  transformação total para overlay/hambúrguer abaixo de `md`, e
  *content-first* nas telas de conteúdo (grids colapsam para 1 coluna,
  `flex-wrap` em cabeçalhos de ação).
  Breakpoints usados: `sm` (640px), `md` (768px, transição sidebar↔mobile
  nav), `lg` (1024px, colunas assimétricas de landing/dashboard), `xl`
  (telas de autenticação).
- Nenhuma tabela de largura fixa: `Table` sempre dentro de um contêiner com
  `overflow-x-auto`.
- Textos truncam (`truncate`) apenas em linhas de lista de largura
  controlada (nunca em títulos de página H1).

## 11. Acessibilidade

- Todo controle de foco mantém `focus-visible:ring` visível — nunca
  `outline: none` sem substituto.
- Contraste de texto segue AA nos pares de tokens padrão (§3.3).
- Ícones isolados (botões de ícone) precisam de `sr-only`/`aria-label`
  (padrão já usado no botão de fechar do `Dialog`).
- `<details>/<summary>` do FAQ é navegável por teclado nativamente, sem JS
  customizado.
- Elementos desabilitados por serem funcionalidade futura ("Em breve") usam
  `aria-disabled="true"` em vez de link real, para não simular uma navegação
  que não leva a lugar nenhum.

## 12. O que não foi alterado

Esta tarefa é puramente visual/UX. Não foram alterados: contratos de API,
schema/migrações do banco, regras de autorização, lógica de negócio dos
serviços do backend, ou o comportamento funcional de nenhuma tela (todas as
chamadas `apiFetch`, validações Zod e fluxos permanecem os mesmos — apenas a
camada de apresentação mudou).
