import Link from "next/link";
import { Cpu } from "lucide-react";
import { AppCard } from "@/components/ui/app-card";
import { ProgressBar } from "@/components/ui/progress-bar";
import { StatusBadge } from "@/components/status-badge";
import { EmptyState } from "@/components/ui/empty-state";
import type { AiProcessingItem } from "@/types/dashboard";

/**
 * Painel de processamentos de IA (transcrição + geração de questões).
 * Funcionalidade real chega na Fase 3 — aqui exibimos apenas a estrutura
 * visual com dados de demonstração, claramente identificados.
 */
export function AiProcessingPanel({ items }: { items: AiProcessingItem[] }) {
  return (
    <AppCard tone="surface" className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-medium">Processamentos de IA</h2>
          <p className="text-xs text-muted-foreground">Transcrição e geração de questões · dados de demonstração</p>
        </div>
        <Link href="#" aria-disabled="true" className="text-xs font-medium text-muted-foreground/60">
          Ver todos
        </Link>
      </div>

      {items.length === 0 ? (
        <EmptyState icon={Cpu} title="Nenhum processamento em andamento" />
      ) : (
        <ul className="flex flex-col divide-y divide-border/70">
          {items.map((item) => (
            <li key={item.id} className="flex flex-col gap-2 py-3 first:pt-0 last:pb-0">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <p className="truncate text-sm font-medium">{item.title}</p>
                  <p className="truncate text-xs text-muted-foreground">{item.description}</p>
                </div>
                <StatusBadge status={item.status} className="shrink-0" />
              </div>
              <div className="flex items-center gap-2.5">
                <ProgressBar value={item.progress} aria-label={`Progresso de ${item.title}`} className="flex-1" />
                <span className="w-9 shrink-0 text-right text-xs tabular-nums text-muted-foreground">
                  {item.progress}%
                </span>
              </div>
            </li>
          ))}
        </ul>
      )}
    </AppCard>
  );
}
