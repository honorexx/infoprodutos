import { cn } from "@/lib/utils";

/**
 * Marca "Infoprodutos": três hastes ascendentes (progresso/crescimento) com o
 * destaque em verde na haste mais alta. Ver docs/DESIGN_SYSTEM.md#marca.
 */
export function LogoMark({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
      className={cn("size-4", className)}
    >
      <rect x="3" y="13" width="4" height="8" rx="1" fill="currentColor" opacity="0.45" />
      <rect x="10" y="8" width="4" height="13" rx="1" fill="currentColor" opacity="0.75" />
      <rect x="17" y="3" width="4" height="18" rx="1" className="fill-accent" />
    </svg>
  );
}

export function Logo({
  className,
  markClassName,
  showWordmark = true,
}: {
  className?: string;
  markClassName?: string;
  showWordmark?: boolean;
}) {
  return (
    <span className={cn("inline-flex items-center gap-2.5", className)}>
      <span
        className={cn(
          "flex size-8 shrink-0 items-center justify-center rounded-md bg-foreground text-background",
          markClassName,
        )}
      >
        <LogoMark className="size-4" />
      </span>
      {showWordmark && (
        <span className="font-heading text-[1.05rem] leading-none font-medium tracking-tight">
          Infoprodutos
        </span>
      )}
    </span>
  );
}
