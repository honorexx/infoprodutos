"use client";

import Image from "next/image";
import { cn } from "@/lib/utils";

type EditorialPhotoProps = {
  src: string;
  alt: string;
  objectPosition: string;
  sizes: string;
  priority?: boolean;
  className?: string;
  /** Leve correção visual — sem esconder a fotografia. */
  tone?: "none" | "subtle";
};

/**
 * Foto editorial PKS: quality 95 + sizes retina.
 * Fontes PNG em /public/images/pks (sem JPEG comprimido).
 */
export function EditorialPhoto({
  src,
  alt,
  objectPosition,
  sizes,
  priority = false,
  className,
  tone = "subtle",
}: EditorialPhotoProps) {
  return (
    <Image
      src={src}
      alt={alt}
      fill
      priority={priority}
      quality={95}
      sizes={sizes}
      className={cn(
        "object-cover",
        tone === "subtle" && "[filter:saturate(0.97)_contrast(1.015)]",
        className,
      )}
      style={{ objectPosition }}
    />
  );
}
