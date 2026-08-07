# DESIGN_SYSTEM — Official Brand Color System — v1

Status: **Fonte da verdade visual**  
Versão: **1.0.0** (2026-08-07)  
Escopo: `apps/web` (camada visual). Sem alteração de contratos de API, banco ou regras de negócio.

> **INEGOCIÁVEL**  
> A identidade cromática oficial é **azul-marinho profundo + dourado champagne**.  
> Não reinterpretar, substituir, aproximar ou “melhorar” os HEX abaixo.  
> Qualquer mudança futura exige decisão explícita registrada em `DECISIONS.md`.

---

## 1. Identidade

| Papel | Valor | Nota |
|---|---|---|
| Cor principal da marca | `#BA9364` (`--gold-500` / `--primary`) | CTA, foco, progresso, assinatura |
| Fundo principal da marca | `#040A16` (`--navy-950` / `--background`) | Canvas da aplicação |

Sensação desejada: premium, executiva, sofisticada, tecnológica, séria, minimalista.  
Composição aproximada: **80% navy/superfícies**, **15% texto/cinza**, **5% dourado**.

O dourado é assinatura — nunca fundo de grandes áreas.

---

## 2. Paleta oficial (HEX)

```css
--navy-950: #040A16;
--navy-900: #07101F;
--navy-850: #0C131F;
--navy-800: #121C2B;
--navy-700: #192538;

--gold-600: #8C7459;
--gold-500: #BA9364;
--gold-400: #CFAE83;
--gold-300: #E0C49F;

--white: #F9FAFC;
--off-white: #F3F1ED;

--slate-300: #A7ADBA;
--slate-400: #858E9E;
--slate-600: #596273;
```

Implementação canônica: `apps/web/src/app/globals.css` (`:root`).

---

## 3. Tokens semânticos de marca

Implementar **exatamente** (também em `globals.css`):

```css
--background: #040A16;
--foreground: #F9FAFC;

--surface: #0C131F;
--surface-hover: #121C2B;
--surface-elevated: #121C2B;

--primary: #BA9364;
--primary-hover: #CFAE83;
--primary-muted: #8C7459;
--primary-foreground: #040A16;

--muted-foreground: #A7ADBA;

--border: rgba(255, 255, 255, 0.07);
--border-gold: rgba(186, 147, 100, 0.20);
--border-gold-active: rgba(186, 147, 100, 0.50);

--focus-ring: #BA9364;
```

### 3.1 Uso por superfície

| Uso | Token / valor |
|---|---|
| Background principal | `#040A16` |
| Sidebar | `#BA9364` (mesmo dourado do botão primário); texto `#040A16` / muted `#192538` para contraste |
| Cards / superfícies | `#0C131F` |
| Superfície elevada | `#121C2B` |
| Hover de superfície | `#192538` (`--navy-700`) |
| Texto principal | `#F9FAFC` |
| Texto secundário | `#A7ADBA` |
| Texto terciário / placeholder | `#858E9E` |
| Brand / gold | `#BA9364` |
| Gold hover | `#CFAE83` |
| Gold discreto | `#8C7459` |
| Gold detalhe | `#E0C49F` |
| Bordas padrão | `rgba(255,255,255,0.07)` |
| Bordas douradas suaves (hierarquia) | `rgba(186,147,100,0.14)` util `.border-gold-subtle` |
| Bordas douradas de destaque | `rgba(186,147,100,0.45)` util `.border-gold-emphasis` |

### 3.2 Onde usar dourado

Logo; item ativo da navegação; botão principal; ícones importantes; progresso; métricas de destaque; linhas decorativas; selecionado; foco; detalhes de gráfico; pequenas bordas; títulos especiais (palavras/números, não blocos longos).

### 3.3 Proibido na identidade

Roxo, violeta, azul elétrico/royal, cyan, rosa, verde neon, gradientes multicoloridos, glassmorphism, neon, glow dourado, fundo preto puro `#000000`, amarelo saturado, gradiente azul+roxo.

Gradiente só se muito sutil e só na família navy (ex.: `#040A16` → `#0C131F`).

---

## 4. Componentes (regras)

### Botão primário
- bg `#BA9364`, text `#040A16`
- hover `#CFAE83`, active `#8C7459`
- Reservado a ações principais (`Criar curso`, `Publicar`, etc.)

### Botão secundário
- transparent, text `#F9FAFC`, border `rgba(255,255,255,0.12)`
- hover bg `#121C2B`

### Botão outline gold
- transparent, text `#CFAE83`, border `rgba(186,147,100,0.45)`

### Sidebar
- fundo: `#BA9364` (assinatura alinhada ao CTA primário)
- título / item ativo: `#040A16`
- item normal / metadados: `#192538`
- hover: `#CFAE83` com texto navy
- ativo: overlay navy `rgba(4,10,22,0.12)` + `border-left: 2px solid #040A16`

### Cards
- normal: bg `#0C131F`, border branca 7%
- importantes: bg `#121C2B`, border gold 14% — não em todos

### Inputs
- bg `#07101F`, border branca 10%, text `#F9FAFC`
- placeholder `#858E9E`
- focus border `#BA9364`, ring `rgba(186,147,100,0.20)`

### Progresso
- trilha `rgba(255,255,255,0.08)`
- valor `#BA9364` (nunca verde para progresso normal)

---

## 5. Cores semânticas (não-marca)

Permitidas **somente** para SUCCESS / WARNING / ERROR / INFO — dessaturadas e harmônicas com navy.  
Não fazem parte da identidade. Valores atuais em `globals.css`:

| Token | Valor |
|---|---|
| `--success` | `#5C7A66` |
| `--warning` | `#A68B5B` |
| `--info` | `#6B7A8F` |
| `--danger` | `#A65D5D` |

---

## 6. Tipografia (mantida)

| Papel | Fonte |
|---|---|
| Heading | Fraunces |
| Interface | Public Sans |
| Mono | Geist Mono |

Carregadas em `src/app/layout.tsx` via `next/font/google`.

---

## 7. Regras de implementação

1. Tokens vivem em `globals.css`; Tailwind consome via `@theme inline`.
2. Componentes usam classes semânticas (`bg-primary`, `text-muted-foreground`, `border-border-gold`) — **não** HEX solto no JSX.
3. Novos componentes devem seguir esta regra.
4. Auditoria periódica: buscar HEX hardcoded, `rgb()/hsl()`, classes `blue-*` / `purple-*` / `violet-*` / `cyan-*` / `yellow-*`.
5. **Não alterar esta paleta** sem decisão explícita em `DECISIONS.md`.

---

## 8. Histórico

| Data | Evento |
|---|---|
| 2026-08-06 | Sistema visual editorial anterior (off-white + tinta + verde) |
| 2026-08-07 | **v1 oficial** — navy `#040A16` + gold champagne `#BA9364` (este documento) |
