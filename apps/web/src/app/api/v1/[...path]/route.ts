import { NextRequest, NextResponse } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

function upstreamBase(): string | null {
  const raw = process.env.API_UPSTREAM_URL?.replace(/\/$/, "");
  return raw && raw.length > 0 ? raw : null;
}

async function proxy(request: NextRequest, pathSegments: string[]): Promise<NextResponse> {
  const base = upstreamBase();
  if (!base) {
    return NextResponse.json(
      {
        detail:
          "API_UPSTREAM_URL não configurada na Vercel. Defina a URL HTTPS da API (ex. ngrok/Render).",
      },
      { status: 503 },
    );
  }

  const incoming = new URL(request.url);
  const target = `${base}/api/v1/${pathSegments.join("/")}${incoming.search}`;

  const headers = new Headers();
  request.headers.forEach((value, key) => {
    const lower = key.toLowerCase();
    if (lower === "host" || lower === "connection" || lower === "content-length") return;
    headers.set(key, value);
  });
  // Free ngrok interstitial
  if (base.includes("ngrok")) {
    headers.set("ngrok-skip-browser-warning", "true");
  }

  const init: RequestInit = {
    method: request.method,
    headers,
    redirect: "manual",
  };
  if (request.method !== "GET" && request.method !== "HEAD") {
    init.body = await request.arrayBuffer();
  }

  const upstream = await fetch(target, init);
  const outHeaders = new Headers();
  upstream.headers.forEach((value, key) => {
    const lower = key.toLowerCase();
    if (lower === "transfer-encoding" || lower === "connection") return;
    // Preserve Set-Cookie for refresh token on the Vercel domain
    outHeaders.append(key, value);
  });

  return new NextResponse(upstream.body, {
    status: upstream.status,
    headers: outHeaders,
  });
}

type Ctx = { params: Promise<{ path: string[] }> };

export async function GET(request: NextRequest, ctx: Ctx) {
  const { path } = await ctx.params;
  return proxy(request, path);
}
export async function POST(request: NextRequest, ctx: Ctx) {
  const { path } = await ctx.params;
  return proxy(request, path);
}
export async function PUT(request: NextRequest, ctx: Ctx) {
  const { path } = await ctx.params;
  return proxy(request, path);
}
export async function PATCH(request: NextRequest, ctx: Ctx) {
  const { path } = await ctx.params;
  return proxy(request, path);
}
export async function DELETE(request: NextRequest, ctx: Ctx) {
  const { path } = await ctx.params;
  return proxy(request, path);
}
export async function OPTIONS(request: NextRequest, ctx: Ctx) {
  const { path } = await ctx.params;
  return proxy(request, path);
}
