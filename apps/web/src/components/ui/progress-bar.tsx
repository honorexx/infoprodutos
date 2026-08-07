"use client";

import { motion } from "framer-motion";
import { cn } from "@/lib/utils";
import { progressTransition } from "@/lib/animations";

/**
 * Barra de progresso animada com Framer Motion — a animação de preenchimento
 * roda apenas quando `value` muda (não em loop). Distinta do `Progress`
 * (Radix) em `ui/progress.tsx`; esta versão é usada nos painéis do dashboard
 * (processamento de IA, conclusão de curso) onde o token de cor pode variar.
 */
export function ProgressBar({
  value,
  className,
  trackClassName,
  indicatorClassName,
  "aria-label": ariaLabel,
}: {
  value: number;
  className?: string;
  trackClassName?: string;
  indicatorClassName?: string;
  "aria-label"?: string;
}) {
  const clamped = Math.min(100, Math.max(0, value));

  return (
    <div
      role="progressbar"
      aria-valuenow={clamped}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-label={ariaLabel}
      className={cn("h-1.5 w-full overflow-hidden rounded-full bg-muted", trackClassName, className)}
    >
      <motion.div
        className={cn("h-full rounded-full bg-primary", indicatorClassName)}
        initial={false}
        animate={{ width: `${clamped}%` }}
        transition={progressTransition}
      />
    </div>
  );
}
