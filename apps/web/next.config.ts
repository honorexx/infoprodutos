import type { NextConfig } from "next";

const isVercel = Boolean(process.env.VERCEL);

const nextConfig: NextConfig = {
  // Standalone só para Docker local/prod self-host. Na Vercel o runtime próprio quebra com isso.
  ...(isVercel ? {} : { output: "standalone" as const }),
  // Sem isso, abrir o site pelo domínio do ngrok bloqueia JS/CSS do dev server
  // → página “apagada” e login em loading infinito.
  allowedDevOrigins: [
    "outsmart-diner-fretful.ngrok-free.dev",
    "*.ngrok-free.dev",
    "*.ngrok-free.app",
    "*.ngrok.io",
  ],
  // Proxy local/ngrok: browser → same-origin /api/v1 → Spring. Na Vercel use NEXT_PUBLIC_API_URL absoluto.
  async rewrites() {
    if (isVercel) {
      return [];
    }
    const apiOrigin =
      process.env.API_PROXY_ORIGIN?.replace(/\/$/, "") ?? "http://127.0.0.1:8090";
    return [
      {
        source: "/api/v1/:path*",
        destination: `${apiOrigin}/api/v1/:path*`,
      },
    ];
  },
  images: {
    // Permite quality={95|100} nas fotos de marca (default só tinha 75).
    qualities: [75, 90, 95, 100],
    // Garante variantes retina para hero/split (~50–100vw).
    deviceSizes: [640, 750, 828, 1080, 1200, 1440, 1920, 2048, 2560],
    formats: ["image/avif", "image/webp"],
  },
};

export default nextConfig;
