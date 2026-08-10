# Upload de vídeos longos (Cloudflare R2 / S3)

Em produção o front fica na **Vercel** e a API no **Render**. Enviar vídeos de 20+ min pelo proxy `/api/v1` da Vercel **não funciona** (limite de body ~4,5 MB e timeout curto).

Solução definitiva: **upload direto do browser para object storage** (Cloudflare R2 recomendado) com URL assinada. A API só inicia/confirma e grava metadados.

## Fluxo

1. `POST /api/v1/videos/upload-init` (JSON pequeno, via Vercel OK) → retorna `uploadMode: "DIRECT"` + URLs PUT assinadas
2. Browser faz `PUT` do vídeo e da thumbnail **direto no R2**
3. `POST /api/v1/videos/{id}/upload-complete` → API valida objetos (`HeadObject`) e associa à aula
4. Playback: `GET .../stream-url` devolve **presigned GET** do R2 (aluno toca direto no storage)

Sem R2 configurado, a API cai no modo `PROXY` (multipart na API). O front usa `NEXT_PUBLIC_API_UPLOAD_URL` apontando para o Render — disco do Render free é **efêmero** e **não** serve para produção de aulas longas.

## 1. Criar bucket no Cloudflare R2

1. [dash.cloudflare.com](https://dash.cloudflare.com) → **R2** → Create bucket (ex.: `infoprodutos-videos`)
2. **Manage R2 API Tokens** → Create API token com permissão de Object Read & Write no bucket
3. Anote: **Access Key ID**, **Secret Access Key**, **Endpoint** (ex.: `https://<ACCOUNT_ID>.r2.cloudflarestorage.com`)

### CORS do bucket (obrigatório)

Em R2 → bucket → Settings → CORS:

```json
[
  {
    "AllowedOrigins": [
      "https://www.pksconsultoria.com.br",
      "https://pksconsultoria.com.br",
      "http://localhost:3000"
    ],
    "AllowedMethods": ["GET", "PUT", "HEAD"],
    "AllowedHeaders": ["*"],
    "ExposeHeaders": ["ETag", "Content-Type"],
    "MaxAgeSeconds": 3600
  }
]
```

Bucket **privado** (sem acesso público). Leitura só via URL assinada.

## 2. Variáveis no Render (`infoprodutos-api`)

| Variável | Exemplo |
| --- | --- |
| `VIDEO_S3_ENABLED` | `true` |
| `VIDEO_S3_ENDPOINT` | `https://<ACCOUNT_ID>.r2.cloudflarestorage.com` |
| `VIDEO_S3_REGION` | `auto` |
| `VIDEO_S3_BUCKET` | `infoprodutos-videos` |
| `VIDEO_S3_ACCESS_KEY` | *(token R2)* |
| `VIDEO_S3_SECRET_KEY` | *(secret R2)* |
| `VIDEO_STORAGE_MAX_FILE_BYTES` | `2147483648` (2 GiB) |
| `CORS_ALLOWED_ORIGINS` | `https://pksconsultoria.com.br,https://www.pksconsultoria.com.br,...` |

Reinicie o serviço após salvar.

## 3. Variáveis na Vercel (web)

| Variável | Valor |
| --- | --- |
| `NEXT_PUBLIC_API_URL` | `/api/v1` (same-origin + proxy; cookies OK) |
| `NEXT_PUBLIC_API_UPLOAD_URL` | `https://infoprodutos-api.onrender.com/api/v1` (fallback PROXY sem R2) |
| `API_UPSTREAM_URL` | `https://infoprodutos-api.onrender.com` |

Com R2 ligado, o vídeo **não** usa `NEXT_PUBLIC_API_UPLOAD_URL` — só as URLs assinadas do R2.

## 4. Testar aula de 20+ min

1. Exporte/compresse em **MP4 H.264** (evite DVR `.mov` bruto de gameplay — costuma ser enorme)
2. Login como instrutor → curso → aula → **Enviar vídeo** + thumbnail
3. Barra de progresso deve avançar; ao terminar, a aula mostra vídeo associado
4. Área do aluno: player deve carregar (URL do R2 na aba Network)

## Limites

- Default API: **2 GiB** (`VIDEO_STORAGE_MAX_FILE_BYTES`)
- Free R2: veja cota atual na Cloudflare; tráfego de saída tem free tier generoso
- Sem R2: disco Render some no redeploy — **não use PROXY em produção para aulas**
