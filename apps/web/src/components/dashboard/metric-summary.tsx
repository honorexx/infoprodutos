import { AppCard } from "@/components/ui/app-card";
import type { SecondaryMetric } from "@/types/dashboard";

/**
 * Métricas secundárias, mais compactas que o painel principal. Cada item
 * usa uma composição levemente diferente (ícone+valor, anel, texto simples)
 * para evitar a repetição de "quatro cartões idênticos".
 */
export function MetricSummary({ metrics }: { metrics: SecondaryMetric[] }) {
  return (
    <div className="grid gap-3 sm:grid-cols-3">
      {metrics.map((metric, index) => (
        <AppCard key={metric.id} tone={index === 1 ? "elevated" : "surface"} className="flex items-center gap-4">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary-soft text-primary-soft-foreground">
            <metric.icon className="size-4.5" />
          </span>
          <div className="min-w-0">
            <p className="font-serif text-2xl leading-none font-medium tracking-tight">{metric.value}</p>
            <p className="mt-1 truncate text-xs text-muted-foreground">{metric.label}</p>
            {metric.helper && <p className="mt-0.5 truncate text-[11px] text-muted-foreground/80">{metric.helper}</p>}
          </div>
        </AppCard>
      ))}
    </div>
  );
}
