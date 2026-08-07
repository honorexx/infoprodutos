import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

/**
 * Composições comuns de `Skeleton` para estados de carregamento, preparadas
 * para uso quando os painéis do dashboard passarem a consumir dados reais.
 */
export function LoadingSkeleton({
  variant = "card",
  count = 1,
  className,
}: {
  variant?: "card" | "row" | "text" | "metric";
  count?: number;
  className?: string;
}) {
  const items = Array.from({ length: count });

  if (variant === "row") {
    return (
      <div className={cn("flex flex-col gap-2", className)}>
        {items.map((_, i) => (
          <Skeleton key={i} className="h-12 w-full" />
        ))}
      </div>
    );
  }

  if (variant === "text") {
    return (
      <div className={cn("flex flex-col gap-2", className)}>
        {items.map((_, i) => (
          <Skeleton key={i} className={cn("h-3", i === items.length - 1 ? "w-2/3" : "w-full")} />
        ))}
      </div>
    );
  }

  if (variant === "metric") {
    return (
      <div className={cn("grid gap-3 sm:grid-cols-3", className)}>
        {items.map((_, i) => (
          <Skeleton key={i} className="h-24 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  return (
    <div className={cn("flex flex-col gap-4", className)}>
      {items.map((_, i) => (
        <Skeleton key={i} className="h-40 w-full rounded-lg" />
      ))}
    </div>
  );
}
