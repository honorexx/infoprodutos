# Infoprodutos — Web

Frontend em Next.js (App Router) da plataforma Infoprodutos. Ver o [`README.md`](../../README.md) da raiz do monorepo para instruções completas de execução, e [`/docs`](../../docs) para a documentação técnica do projeto.

## Comandos

```bash
pnpm install       # instalar dependências
pnpm dev           # servidor de desenvolvimento (http://localhost:3000)
pnpm build         # build de produção
pnpm start         # servir o build de produção
pnpm lint          # eslint
pnpm test          # testes (Vitest)
pnpm test:watch    # testes em modo watch
```

## Variáveis de ambiente

Copie `.env.example` para `.env.local` e ajuste `NEXT_PUBLIC_API_URL` para apontar para a API do backend.

## Estrutura

- `src/app` — rotas (App Router): `/`, `/login`, `/register`, `/dashboard`, `/admin/users`.
- `src/components` — componentes de UI (`components/ui` = shadcn/ui) e componentes de aplicação (`site-header`, `protected-route`).
- `src/lib` — `api-client.ts` (wrapper fetch com renovação automática de token), `auth-context.tsx` (estado de autenticação), `validation.ts` (schemas Zod), `types.ts`.
