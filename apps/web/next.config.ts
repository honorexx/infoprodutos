import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Gera uma saída "standalone" para permitir uma imagem Docker enxuta,
  // sem precisar copiar node_modules inteiro para o estágio final.
  output: "standalone",
};

export default nextConfig;
