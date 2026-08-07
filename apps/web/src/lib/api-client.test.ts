import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { apiFetch, ApiError } from "@/lib/api-client";
import { getAccessToken, setAccessToken } from "@/lib/token-store";

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("apiFetch", () => {
  beforeEach(() => {
    setAccessToken(null);
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("retorna o corpo JSON em uma resposta bem-sucedida", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, { hello: "world" }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await apiFetch<{ hello: string }>("/ping");

    expect(result).toEqual({ hello: "world" });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("inclui o header Authorization quando há access token", async () => {
    setAccessToken("token-123");
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, {}));
    vi.stubGlobal("fetch", fetchMock);

    await apiFetch("/secure");

    const [, requestInit] = fetchMock.mock.calls[0];
    expect(requestInit.headers.Authorization).toBe("Bearer token-123");
  });

  it("lança ApiError com o corpo padronizado em respostas de erro", async () => {
    const errorBody = {
      type: "validation-error",
      title: "Erro de validação",
      status: 400,
      detail: "Campos inválidos",
      instance: "/x",
      timestamp: "2026-01-01T00:00:00Z",
      correlationId: "abc",
      errors: [],
    };
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(400, errorBody));
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiFetch("/fails")).rejects.toMatchObject({
      status: 400,
      body: errorBody,
    });
  });

  it("tenta renovar o token em uma resposta 401 e repete a requisição original", async () => {
    const fetchMock = vi
      .fn()
      // primeira chamada: 401
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      // chamada de refresh: sucesso
      .mockResolvedValueOnce(jsonResponse(200, { accessToken: "new-token" }))
      // repetição da chamada original: sucesso
      .mockResolvedValueOnce(jsonResponse(200, { ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await apiFetch<{ ok: boolean }>("/needs-auth");

    expect(result).toEqual({ ok: true });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(getAccessToken()).toBe("new-token");
  });

  it("propaga o erro quando o refresh também falha", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiFetch("/needs-auth")).rejects.toBeInstanceOf(ApiError);
    expect(getAccessToken()).toBeNull();
  });
});
