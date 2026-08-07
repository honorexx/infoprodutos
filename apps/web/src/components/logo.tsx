import Image from "next/image";
import { cn } from "@/lib/utils";

/** Assets — monograma PKS (sem Consultoria). */
const LOGO_GOLD = "/brand/pks-logo-transparent.png";
const LOGO_INK = "/brand/pks-logo-ink.png";
const MARK_GOLD = "/brand/pks-logo-mark.png";
const MARK_INK = "/brand/pks-logo-mark-ink.png";
const MONO_INK = "/brand/pks-monogram-ink.png";
const MONO_GOLD = "/brand/pks-monogram.png";

/**
 * Marca compacta (quadrado) — mobile header / collapsed.
 * Preferir `SidebarBrand` na sidebar expandida (mais nítida).
 */
export function LogoMark({
  className,
  variant = "gold",
}: {
  className?: string;
  variant?: "gold" | "ink";
}) {
  const src = variant === "ink" ? MARK_INK : MARK_GOLD;
  return (
    <span className={cn("relative inline-flex size-11 shrink-0 overflow-hidden", className)}>
      <Image
        src={src}
        alt=""
        fill
        sizes="88px"
        quality={100}
        priority
        unoptimized
        className="object-contain"
      />
    </span>
  );
}

/**
 * Monograma PKS horizontal nítido — sidebar dourada (alinhado à esquerda).
 */
export function SidebarBrand({
  className,
  collapsed = false,
}: {
  className?: string;
  collapsed?: boolean;
}) {
  if (collapsed) {
    return <LogoMark variant="ink" className={cn("size-10", className)} />;
  }

  return (
    <span className={cn("inline-flex items-center justify-center", className)}>
      <Image
        src={MONO_INK}
        alt="PKS"
        width={160}
        height={81}
        sizes="200px"
        quality={100}
        priority
        unoptimized
        className="h-10 w-auto max-w-[11rem] object-contain object-center"
      />
    </span>
  );
}

export function Logo({
  className,
  priority = false,
  size = "md",
  variant = "gold",
}: {
  className?: string;
  markClassName?: string;
  showWordmark?: boolean;
  priority?: boolean;
  size?: "sm" | "md" | "lg";
  variant?: "gold" | "ink";
}) {
  const dims = {
    sm: { width: 168, height: 114, className: "h-12 w-auto" },
    md: { width: 200, height: 136, className: "h-14 w-auto" },
    lg: { width: 240, height: 163, className: "h-16 w-auto sm:h-[4.5rem]" },
  }[size];

  const src = variant === "ink" ? LOGO_INK : LOGO_GOLD;

  return (
    <span className={cn("inline-flex items-center", className)}>
      <Image
        src={src}
        alt="PKS Consultoria"
        width={dims.width}
        height={dims.height}
        sizes={`${dims.width * 2}px`}
        quality={100}
        priority={priority}
        unoptimized
        className={cn("object-contain", dims.className)}
      />
    </span>
  );
}

/** @internal re-export paths if needed */
export const brandAssets = { MONO_GOLD, MONO_INK };
