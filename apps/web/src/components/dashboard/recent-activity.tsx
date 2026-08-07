import { AppCard } from "@/components/ui/app-card";
import { EmptyState } from "@/components/ui/empty-state";
import type { ActivityItem } from "@/types/dashboard";
import { Clock } from "lucide-react";

export function RecentActivity({ items }: { items: ActivityItem[] }) {
  return (
    <AppCard tone="surface" className="flex flex-col gap-4">
      <div>
        <h2 className="text-sm font-medium">Atividade recente</h2>
        <p className="text-xs text-muted-foreground">Dados de demonstração</p>
      </div>

      {items.length === 0 ? (
        <EmptyState icon={Clock} title="Nenhuma atividade recente" />
      ) : (
        <ul className="flex flex-col divide-y divide-border/70">
          {items.map((item) => {
            const Icon = item.icon;
            return (
              <li key={item.id} className="flex items-start gap-3 py-3 first:pt-0 last:pb-0">
                <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary-soft text-[11px] font-semibold text-primary-soft-foreground">
                  {item.initials}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm">
                    <span className="font-medium">{item.description}</span>
                    <span className="text-muted-foreground"> · {item.courseTitle}</span>
                  </p>
                  <p className="mt-0.5 flex items-center gap-1.5 text-xs text-muted-foreground">
                    <Icon className="size-3" />
                    {item.relativeTime}
                  </p>
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </AppCard>
  );
}
