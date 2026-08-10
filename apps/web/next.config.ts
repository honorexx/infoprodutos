import type { NextConfig } from "next";

const isVercel = Boolean(process.env.VERCEL);

const nextConfig: NextConfig = {
  // Standalone só para Docker. Na Vercel o runtime próprio quebra com isso.
  ...(isVercel ? {} : { output: "standalone" as const }),
  allowedDevOrigins: [
    "outsmart-diner-fretful.ngrok-free.dev",
    "*.ngrok-free.dev",
    "*.ngrok-free.app",
    "*.ngrok.io",
  ],
  // Browser sempre chama /api/v1 (same-origin). Next faz proxy para a Spring.
  // Local/ngrok: 127.0.0.1:8090. Vercel: defina API_UPSTREAM_URL (HTTPS público da API).
  async rewrites() {
    const upstream =
      process.env.API_UPSTREAM_URL?.replace(/\/$/, "") ||
      (!isVercel ? "http://127.0.0.1:8090" : "");
    if (!upstream) {
      return [];
    }
    return [
      {
        source: "/api/v1/:path*",
        destination: `${upstream}/api/v1/:path*`,
      },
    ];
  },
  images: {
    qualities: [75, 90, 95, 100],
    deviceSizes: [640, 750, 828, 1080, 1200, 1440, 1920, 2048, 2560],
    formats: ["image/avif", "image/webp"],
  },
};

export default nextConfig;
