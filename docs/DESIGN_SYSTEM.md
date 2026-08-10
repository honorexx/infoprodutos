# PKS Design System v1

Status: **Fonte da verdade visual**  
Versão: **1.2.0** (2026-08-07)  
Escopo: `apps/web` (camada visual). Sem alteração de contratos de API, banco ou regras de negócio.

> **INEGOCIÁVEL**  
> O sistema cromático oficial da PKS Consultoria é **NAVY + GOLD**  
> (**azul-marinho profundo + dourado champagne**).  
> Não reinterpretar, substituir, aproximar ou “melhorar” os HEX abaixo.  
> Qualquer mudança futura exige decisão explícita registrada em `DECISIONS.md`.

---

## 1. Identidade

| Papel | Valor | Nota |
|---|---|---|
| Cor principal da marca | `#BA9364` (`--gold-500` / `--primary`) | CTA, foco, progresso, assinatura |
| Fundo principal da marca | `#040A16` (`--navy-950` / `--background`) | Canvas — **nunca** `#000000` |

Sensação: premium, executiva, sofisticada, tecnológica, séria, minimalista.  
Composição: **75–80% navy**, **15–20% branco/cinza**, **5% dourado**.

Duas experiências, mesma marca: **site público editorial** + **LMS logado**.

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

Implementação: `apps/web/src/app/globals.css` (`:root`).

---

## 3. Tokens semânticos

```css
--background: #040A16;
--background-secondary: #07101F;
--foreground: #F9FAFC;

--surface: #0C131F;
--surface-elevated: #121C2B;
--surface-hover: #192538;

--primary: #BA9364;
--primary-hover: #CFAE83;
--primary-muted: #8C7459;
--primary-foreground: #040A16;

--muted-foreground: #A7ADBA;
--subtle-foreground: #858E9E;

--border: rgba(255,255,255,0.07);
--border-gold: rgba(186,147,100,0.20);
--border-gold-active: rgba(186,147,100,0.50);
--focus-ring: #BA9364;
```

### 3.1 Sidebar LMS (navy)

| Token | Valor |
|---|---|
| Fundo | `#040A16` |
| Item normal | `#A7ADBA` |
| Hover texto | `#F9FAFC` + bg `#0C131F` |
| Ativo texto | `#CFAE83` |
| Ativo bg | `rgba(186,147,100,.08)` |
| Indicador | barra lateral dourada `2px` |

**Não** pintar a sidebar de dourado sólido.

### 3.2 Onde usar dourado

Logo; item ativo; CTA primário; progresso; números; linhas editoriais; bordas de destaque; foco; ícones importantes.

### 3.3 Proibido

Roxo, violeta, cyan, neon, azul elétrico, glow, glassmorphism exagerado, `#000000` como fundo estrutural, gradientes multicoloridos.

---

## 4. Tipografia

| Papel | Fonte | Uso |
|---|---|---|
| Interface | Public Sans | menus, botões, forms, tabelas, labels |
| Editorial | Fraunces | grandes títulos, saudação, frases estratégicas |
| Mono | Geist Mono | códigos, numeração editorial |

Pesos preferidos: **400 / 500 / 600**. Evitar 800/900.

---

## 5. Spacing, radius, sombra

| Token | Valor |
|---|---|
| `--radius-sm` | `0.25rem` |
| `--radius-md` | `0.375rem` |
| `--radius-lg` | `0.5rem` |
| Sombra | suave, navy — sem multi-layer pesado |

---

## 6. Componentes

### Botão primário
bg `#BA9364`, text `#040A16`; hover `#CFAE83`; active `#8C7459`.

### Botão outline gold
transparent, text `#CFAE83`, border gold active.

### Botão secundário
transparent, text `#F9FAFC`, border branca 12%.

### Cards
bg surface, border branca 7%, radius md. Hover: border gold discreta. **Não** cardificar tudo.

### Inputs
bg `#07101F`, border branca 10%, focus gold.

### Progresso
trilha `rgba(255,255,255,0.08)`; valor `#BA9364` (fina no LMS).

---

## 7. Cores semânticas (não-marca)

| Token | Valor |
|---|---|
| `--success` | `#5C7A66` |
| `--warning` | `#A68B5B` |
| `--info` | `#6B7A8F` |
| `--danger` | `#A65D5D` |

---

## 8. Fotografia

| Uso | Path |
|---|---|
| Hero | `/images/pks/rafael-rooftop.png` |
| Experiência | `/images/pks/pedro-bmw.png` |
| Metodologia | `/images/pks/rafael-mercedes.png` |

Logo: `/brand/pks-logo.png` (proporção preservada). Monograma em espaços estreitos.

Permitido: crop, object-position, overlay navy discreto. Proibido: IA em rostos, blur excessivo, substituir fotos.

---

## 9. Motion

Framer Motion + `reducedMotion="user"`.  
fade + translateY 8–14px · duration 250–500ms · stagger 0.04–0.08s.  
Sem float, pulse, glow ou loop decorativo.

---

## 10. Regras de implementação

1. Tokens em `globals.css`; Tailwind via `@theme inline`.
2. Componentes usam classes semânticas — sem HEX solto no JSX.
3. Mocks só em `src/mocks/*` e só em DEV.
4. Não alterar paleta sem `DECISIONS.md`.

---

## 11. Histórico

| Data | Evento |
|---|---|
| 2026-08-06 | Sistema editorial anterior |
| 2026-08-07 | v1 oficial NAVY + GOLD |
| 2026-08-07 | v1.1 home editorial + fotos |
| 2026-08-07 | **v1.2 PKS Design System** — sidebar navy, tokens semânticos, radius moderado |
