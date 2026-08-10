import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Gera uma saída "standalone" para permitir uma imagem Docker enxuta,
  // sem precisar copiar node_modules inteiro para o estágio final.
  output: "standalone",
  images: {
    // Permite quality={95|100} nas fotos de marca (default só tinha 75).
    qualities: [75, 90, 95, 100],
    // Garante variantes retina para hero/split (~50–100vw).
    deviceSizes: [640, 750, 828, 1080, 1200, 1440, 1920, 2048, 2560],
    formats: ["image/avif", "image/webp"],
  },
};

export default nextConfig;
