import Link from "next/link";
import { Award } from "lucide-react";
import { AppCard } from "@/components/ui/app-card";
import { Button } from "@/components/ui/button";

/**
 * Destaque visual de certificados. Contagem e CTA são demonstrativos —
 * a funcionalidade real chega na Fase 5.
 */
export function CertificateHighlight({ issuedCount }: { issuedCount: number }) {
  return (
    <AppCard tone="elevated" className="relative overflow-hidden">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -right-6 -bottom-8 size-36 rounded-full bg-primary/10"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute right-8 -bottom-10 size-24 rounded-full border border-accent/30"
      />

      <div className="relative flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex items-start gap-3">
          <span className="flex size-10 shrink-0 items-center justify-center rounded-md bg-primary-soft text-primary-soft-foreground">
            <Award className="size-5" />
          </span>
          <div>
            <p className="text-xs font-semibold tracking-[0.14em] text-accent uppercase">Certificados</p>
            <p className="mt-1 font-serif text-3xl leading-none font-medium tracking-tight">
              {issuedCount.toLocaleString("pt-BR")}
            </p>
            <p className="mt-1.5 text-sm text-muted-foreground">
              emitidos · área real disponível na Fase 5
            </p>
          </div>
        </div>
        <Button variant="outline" size="sm" disabled className="w-fit">
          Ver certificados
        </Button>
      </div>
      <p className="relative mt-3 text-[11px] text-muted-foreground">
        Conteúdo de demonstração.{" "}
        <Link href="/courses" className="font-medium text-primary hover:underline">
          Continuar com os cursos
        </Link>
        .
      </p>
    </AppCard>
  );
}
