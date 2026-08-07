import * as React from "react";
import { cn } from "@/lib/utils";

/**
 * Superfície do dashboard com duas variações de "peso":
 * - `surface`: painel discreto, quase no mesmo tom do fundo (para agrupar
 *   conteúdo sem parecer "mais um cartão").
 * - `elevated`: superfície destacada, usada para os poucos elementos que
 *   realmente precisam se sobressair (ex.: painel principal de métricas).
 * - `inverse`: superfície escura (mesma família da sidebar), usada para o
 *   painel de destaque do dashboard mesmo no modo claro.
 *
 * Evite usar `AppCard` para todo bloco pequeno de informação — prefira
 * divisores e tipografia quando o conteúdo não precisar de um contêiner
 * visual próprio (ver docs/DESIGN_SYSTEM.md).
 */
type AppCardTone = "surface" | "elevated" | "inverse";

function toneClassName(tone: AppCardTone) {
  switch (tone) {
    case "elevated":
      return "bg-surface-elevated border-border shadow-soft";
    case "inverse":
      return "bg-sidebar border-sidebar-border text-sidebar-foreground";
    case "surface":
    default:
      return "bg-surface border-border";
  }
}

export function AppCard({
  tone = "surface",
  className,
  ...props
}: React.ComponentProps<"div"> & { tone?: AppCardTone }) {
  return (
    <div
      data-slot="app-card"
      data-tone={tone}
      className={cn("rounded-lg border p-5", toneClassName(tone), className)}
      {...props}
    />
  );
}
