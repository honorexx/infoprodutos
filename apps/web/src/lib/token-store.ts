/**
 * Guarda o access token apenas em memória (nunca em localStorage/sessionStorage)
 * para reduzir a superfície de exposição a ataques XSS. O refresh token vive em
 * um cookie httpOnly gerenciado exclusivamente pelo backend.
 */
let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null): void {
  accessToken = token;
}
