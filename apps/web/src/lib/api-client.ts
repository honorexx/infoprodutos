import { API_BASE_URL, API_UPLOAD_BASE_URL } from "@/lib/config";
import { getAccessToken, setAccessToken } from "@/lib/token-store";
import type { ApiErrorBody } from "@/lib/types";

export class ApiError extends Error {
  readonly status: number;
  readonly body: ApiErrorBody | null;

  constructor(status: number, body: ApiErrorBody | null, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  skipAuth?: boolean;
  /** Uso interno: evita loop infinito de retry no fluxo de refresh. */
  isRetry?: boolean;
}

let refreshPromise: Promise<boolean> | null = null;

/** Evita bootstrap de auth travado quando o upstream/proxy não responde. */
const REFRESH_TIMEOUT_MS = 8_000;

async function refreshAccessToken(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = (async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
          method: "POST",
          credentials: "include",
          signal: AbortSignal.timeout(REFRESH_TIMEOUT_MS),
        });
        if (!response.ok) {
          setAccessToken(null);
          return false;
        }
        const data = await response.json();
        setAccessToken(data.accessToken);
        return true;
      } catch {
        setAccessToken(null);
        return false;
      } finally {
        refreshPromise = null;
      }
    })();
  }
  return refreshPromise;
}

async function parseErrorBody(response: Response): Promise<ApiErrorBody | null> {
  try {
    return (await response.json()) as ApiErrorBody;
  } catch {
    return null;
  }
}

export type UploadInitResponse = {
  videoAssetId: string;
  uploadMode: "DIRECT" | "PROXY" | string;
  uploadUrl: string | null;
  videoUploadUrl: string | null;
  thumbnailUploadUrl: string | null;
  videoContentType: string | null;
  thumbnailContentType: string | null;
  uploadStatus: string;
};

/** PUT para URL assinada (R2/S3). Sem cookies — a assinatura autentica. */
export function putPresigned(
  url: string,
  body: Blob,
  contentType: string,
  onProgress?: (pct: number) => void,
): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("PUT", url);
    xhr.setRequestHeader("Content-Type", contentType);
    xhr.upload.onprogress = (event) => {
      if (!onProgress || !event.lengthComputable || event.total <= 0) return;
      onProgress(Math.min(99, Math.round((event.loaded / event.total) * 100)));
    };
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        onProgress?.(100);
        resolve();
        return;
      }
      reject(new ApiError(xhr.status, null, `Falha no upload direto (${xhr.status}).`));
    };
    xhr.onerror = () => reject(new ApiError(0, null, "Falha de rede no upload direto."));
    xhr.onabort = () => reject(new ApiError(0, null, "Upload cancelado."));
    xhr.send(body);
  });
}

/** Upload multipart (não define Content-Type — o browser envia o boundary). */
export async function apiUpload<T>(
  path: string,
  formData: FormData,
  method: "POST" | "PUT" = "POST",
  options?: { baseUrl?: string; onProgress?: (pct: number) => void },
): Promise<T> {
  const base = options?.baseUrl ?? API_UPLOAD_BASE_URL;
  const onProgress = options?.onProgress;

  const doXhr = (token: string | null): Promise<Response> =>
    new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open(method, `${base}${path}`);
      if (token) {
        xhr.setRequestHeader("Authorization", `Bearer ${token}`);
      }
      xhr.withCredentials = true;
      xhr.upload.onprogress = (event) => {
        if (!onProgress || !event.lengthComputable || event.total <= 0) return;
        onProgress(Math.min(99, Math.round((event.loaded / event.total) * 100)));
      };
      xhr.onload = () => {
        onProgress?.(100);
        resolve(
          new Response(xhr.response, {
            status: xhr.status,
            statusText: xhr.statusText,
            headers: parseRawHeaders(xhr.getAllResponseHeaders()),
          }),
        );
      };
      xhr.onerror = () => reject(new ApiError(0, null, "Falha de rede no upload."));
      xhr.onabort = () => reject(new ApiError(0, null, "Upload cancelado."));
      xhr.responseType = "text";
      xhr.send(formData);
    });

  let response = await doXhr(getAccessToken());
  if (response.status === 401) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      response = await doXhr(getAccessToken());
    }
  }
  if (!response.ok) {
    const errorBody = await parseErrorBody(response);
    throw new ApiError(response.status, errorBody, errorBody?.detail ?? `Erro ${response.status}`);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}

function parseRawHeaders(raw: string): Headers {
  const headers = new Headers();
  raw
    .trim()
    .split(/[\r\n]+/)
    .forEach((line) => {
      const idx = line.indexOf(":");
      if (idx > 0) {
        headers.append(line.slice(0, idx).trim(), line.slice(idx + 1).trim());
      }
    });
  return headers;
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = "GET", body, skipAuth = false, isRetry = false } = options;

  const headers: Record<string, string> = { "Content-Type": "application/json" };
  const token = getAccessToken();
  if (token && !skipAuth) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers,
    credentials: "include",
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (response.status === 401 && !skipAuth && !isRetry) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return apiFetch<T>(path, { ...options, isRetry: true });
    }
  }

  if (!response.ok) {
    const errorBody = await parseErrorBody(response);
    throw new ApiError(response.status, errorBody, errorBody?.detail ?? response.statusText);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export { refreshAccessToken };
