/** Base da API no browser. Preferir `/api/v1` (rewrite no Next) para ngrok/HTTPS. */
export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "") || "/api/v1";

/**
 * Base para multipart PROXY (sem R2). Em produção na Vercel, apontar para a API no Render
 * para não estourar body/timeout do serverless. Com R2 (DIRECT) este valor não é usado no vídeo.
 */
export const API_UPLOAD_BASE_URL =
  process.env.NEXT_PUBLIC_API_UPLOAD_URL?.replace(/\/$/, "") || API_BASE_URL;
