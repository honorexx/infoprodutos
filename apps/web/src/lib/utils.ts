import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** Iniciais para avatar — ignora trechos não-alfabéticos (ex.: "Professor (dev)" → "P"). */
export function getInitials(name: string, max = 2): string {
  const parts = name
    .trim()
    .split(/\s+/)
    .map((part) => part.replace(/[^A-Za-zÀ-ÿ]/g, ""))
    .filter((part) => part.length > 0);
  if (parts.length === 0) return "?";
  return parts
    .slice(0, max)
    .map((part) => part[0]!.toUpperCase())
    .join("");
}

/** Formata centavos BRL → "R$ 1.234,56". */
export function formatBrlFromCents(cents: number): string {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(cents / 100);
}

export function reaisToCents(reais: string | number): number {
  const n = typeof reais === "number" ? reais : Number(reais);
  return Math.round(n * 100);
}
