# Deploy da API no Render + domínio pksconsultoria.com.br

Guia para subir o backend (`apps/api`) com Postgres no [Render](https://render.com), blueprint `render.yaml`, e apontar o domínio **pksconsultoria.com.br**.

## Hostnames recomendados

| Host | Destino |
| --- | --- |
| `https://pksconsultoria.com.br` e `https://www.pksconsultoria.com.br` | Front (Vercel / Next) |
| `https://api.pksconsultoria.com.br` | API (Render) |
| `noreply@pksconsultoria.com.br` | Remetente Resend (após domínio verificado) |

Webhook Mercado Pago:

`https://api.pksconsultoria.com.br/api/v1/payments/mercadopago/webhook`

## 1. Conectar o repositório

1. Render Dashboard → **New** → **Blueprint**.
2. Conecte o repositório deste monorepo e confirme `render.yaml`.
3. Aplique: cria `infoprodutos-db` + serviço Docker `infoprodutos-api`.

## 2. Domínio customizado na API (Render)

1. No serviço `infoprodutos-api` → **Settings** → **Custom Domains** → adicione `api.pksconsultoria.com.br`.
2. No DNS do registrante do `.com.br`, crie o registro que o Render mostrar (em geral **CNAME** `api` → `xxx.onrender.com`).
3. Aguarde TLS ativo (pode levar alguns minutos).

## 3. Variáveis de ambiente (secrets)

No serviço `infoprodutos-api`:

| Variável | Valor sugerido |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `JWT_SECRET` | gerado pelo Render |
| `COOKIE_SECURE` | `true` |
| `MP_MOCK_WHEN_TOKEN_BLANK` | `false` |
| `MP_ACCESS_TOKEN` | token de **produção** do Mercado Pago |
| `MP_WEBHOOK_SECRET` | secret do painel MP (Webhooks) |
| `RESEND_API_KEY` | API key Resend |
| `MAIL_FROM` | `PKS Consultoria <noreply@pksconsultoria.com.br>` |
| `FRONTEND_BASE_URL` | `https://pksconsultoria.com.br` |
| `CORS_ALLOWED_ORIGINS` | `https://pksconsultoria.com.br,https://www.pksconsultoria.com.br` |
| `API_PUBLIC_BASE_URL` | `https://api.pksconsultoria.com.br` |

`DB_*` vêm do blueprint. Nunca commite `.env` de produção.

## 4. Mercado Pago

1. App de **produção** → Webhooks → URL:

   `https://api.pksconsultoria.com.br/api/v1/payments/mercadopago/webhook`

2. Eventos de pagamento (`payment`).
3. Cole o secret em `MP_WEBHOOK_SECRET`. Com secret definido, assinatura inválida → **401**.

As preferences já enviam `notification_url` a partir de `API_PUBLIC_BASE_URL`.

## 5. Resend (e-mail)

1. Em [resend.com](https://resend.com) → Domains → adicione `pksconsultoria.com.br`.
2. Publique os registros DNS (SPF/DKIM) que o Resend indicar.
3. Defina `RESEND_API_KEY` + `MAIL_FROM` acima.
4. Sem chave, a API só loga (ok em local).

## 6. Front (Vercel)

1. Projeto Next → domínio `pksconsultoria.com.br` / `www`.
2. `NEXT_PUBLIC_API_URL=https://api.pksconsultoria.com.br/api/v1`
3. DNS apex/www conforme o Vercel.

## 7. Smoke pós-deploy

```bash
curl -sS -o /dev/null -w "%{http_code}\n" https://api.pksconsultoria.com.br/api-docs
# Sem assinatura válida (com secret setado) → 401
curl -sS -o /dev/null -w "%{http_code}\n" -X POST \
  "https://api.pksconsultoria.com.br/api/v1/payments/mercadopago/webhook?data.id=1&type=payment"
```

Depois: login → compra → webhook → matrícula + sininho + e-mail.

## 8. Vídeos longos (20+ min)

Não envie vídeo pelo proxy da Vercel. Configure **Cloudflare R2** (URLs assinadas):

→ **[docs/DEPLOY_VIDEO_R2.md](./DEPLOY_VIDEO_R2.md)**
