/** Base da API no browser. Preferir `/api/v1` (rewrite no Next) para ngrok/HTTPS. */
export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "") || "/api/v1";
