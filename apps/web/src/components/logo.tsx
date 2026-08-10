import Image from "next/image";
import { cn } from "@/lib/utils";

const LOGO_GOLD = "/brand/pks-logo.png";
const LOGO_TRANSPARENT = "/brand/pks-logo-transparent.png";
const LOGO_INK = "/brand/pks-logo-ink.png";
const MARK_GOLD = "/brand/pks-logo-mark.png";
const MARK_INK = "/brand/pks-logo-mark-ink.png";
const MONO_GOLD = "/brand/pks-monogram.png";
const MONO_INK = "/brand/pks-monogram-ink.png";

/**
 * Marca compacta — header mobile / sidebar recolhida.
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
    <span className={cn("relative inline-flex size-10 shrink-0 overflow-hidden", className)}>
      <Image
        src={src}
        alt=""
        fill
        sizes="80px"
        quality={100}
        priority
        unoptimized
        className="object-contain"
      />
    </span>
  );
}

/**
 * Marca da sidebar navy — logo dourada (não ink).
 */
export function SidebarBrand({
  className,
  collapsed = false,
}: {
  className?: string;
  collapsed?: boolean;
}) {
  if (collapsed) {
    return <LogoMark variant="gold" className={cn("size-9", className)} />;
  }

  return (
    <span className={cn("inline-flex items-center justify-center", className)}>
      <Image
        src={LOGO_GOLD}
        alt="PKS Consultoria"
        width={180}
        height={48}
        sizes="200px"
        quality={100}
        priority
        unoptimized
        className="h-8 w-auto max-w-[10.5rem] object-contain object-left"
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
  size?: "sm" | "md" | "lg" | "xl" | "hero";
  variant?: "gold" | "ink";
}) {
  const dims = {
    sm: { width: 168, height: 114, className: "h-12 w-auto" },
    md: { width: 200, height: 136, className: "h-14 w-auto" },
    lg: { width: 240, height: 163, className: "h-16 w-auto sm:h-[4.5rem]" },
    xl: { width: 420, height: 286, className: "h-36 w-auto max-w-[min(100%,22rem)] sm:h-44" },
    hero: {
      width: 440,
      height: 300,
      className: "h-[min(28vh,15rem)] w-auto max-w-[min(100%,22rem)] xl:h-[min(32vh,17rem)] xl:max-w-[24rem]",
    },
  }[size];

  const src = variant === "ink" ? LOGO_INK : LOGO_TRANSPARENT;

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

export const brandAssets = { MONO_GOLD, MONO_INK, LOGO_GOLD };
