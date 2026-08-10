"use client";

import Link from "next/link";
import { Play } from "lucide-react";
import { Progress } from "@/components/ui/progress";
import { Button } from "@/components/ui/button";
import { ApiImage } from "@/components/ui/api-image";
import { cn } from "@/lib/utils";

const TONE: Record<string, string> = {
  navy: "from-navy-800 to-navy-950",
  gold: "from-primary-muted/40 to-navy-900",
  slate: "from-slate-600/30 to-navy-900",
};

export function CourseCardCompact({
  title,
  subtitle,
  progress,
  href,
  coverImageUrl,
  coverTone = "navy",
  className,
}: {
  title: string;
  subtitle?: string;
  progress: number;
  href: string;
  coverImageUrl?: string | null;
  coverTone?: "navy" | "gold" | "slate";
  className?: string;
}) {
  return (
    <Link
      href={href}
      className={cn(
        "group relative flex min-w-[200px] flex-1 flex-col overflow-hidden rounded-md border border-border bg-surface transition-[border-color,transform] duration-300 hover:border-[rgba(186,147,100,0.35)]",
        className,
      )}
    >
      <div
        className={cn(
          "relative aspect-[16/10] overflow-hidden bg-gradient-to-br",
          TONE[coverTone],
        )}
      >
        <ApiImage
          src={coverImageUrl}
          alt=""
          className="absolute inset-0 size-full object-cover transition-transform duration-500 group-hover:scale-[1.02]"
          fallbackClassName="absolute inset-0"
        />
      </div>
      <div className="flex flex-1 flex-col gap-2 p-3.5">
        <h3 className="font-heading text-[0.9375rem] font-medium tracking-[-0.01em] text-foreground transition-transform duration-300 group-hover:translate-y-[-1px]">
          {title}
        </h3>
        {subtitle && <p className="text-xs text-muted-foreground">{subtitle}</p>}
        <div className="mt-auto pt-2">
          <div className="mb-1.5 flex justify-between text-[11px] text-muted-foreground">
            <span>Progresso</span>
            <span className="font-mono text-primary">{Math.round(progress)}%</span>
          </div>
          <Progress value={progress} className="h-px" />
        </div>
      </div>
    </Link>
  );
}

export function CurrentCoursePanel({
  title,
  category,
  progress,
  nextLesson,
  href,
  coverImageUrl,
}: {
  title: string;
  category: string;
  progress: number;
  nextLesson: string;
  href: string;
  coverImageUrl?: string | null;
}) {
  return (
    <article className="overflow-hidden rounded-md border border-border bg-surface">
      <div className="grid lg:grid-cols-[240px_1fr]">
        <div className="relative min-h-[160px] overflow-hidden bg-gradient-to-br from-navy-800 via-navy-900 to-navy-950 lg:min-h-full">
          <ApiImage
            src={coverImageUrl}
            alt=""
            className="absolute inset-0 size-full object-cover"
            fallbackClassName="absolute inset-0"
          />
          {!coverImageUrl && (
            <div
              aria-hidden
              className="absolute inset-0 opacity-40"
              style={{
                background:
                  "radial-gradient(ellipse at 30% 20%, rgba(186,147,100,0.25), transparent 55%)",
              }}
            />
          )}
          <span className="absolute top-4 left-4 z-10 text-[0.625rem] font-medium tracking-[0.16em] text-primary uppercase">
            Curso em andamento
          </span>
        </div>
        <div className="flex flex-col gap-5 p-5 sm:p-6">
          <div>
            <h2 className="font-heading text-2xl font-medium tracking-[-0.02em] text-foreground">
              {title}
            </h2>
            <p className="mt-1 text-sm text-muted-foreground">{category}</p>
          </div>
          <div>
            <div className="mb-2 flex items-baseline justify-between gap-3">
              <span className="text-sm text-muted-foreground">Progresso</span>
              <span className="font-mono text-sm text-primary">{Math.round(progress)}% concluído</span>
            </div>
            <Progress value={progress} className="h-px" />
          </div>
          <div>
            <p className="text-[0.625rem] tracking-[0.12em] text-subtle-foreground uppercase">
              Próxima aula
            </p>
            <p className="mt-1 text-sm text-foreground">{nextLesson}</p>
          </div>
          <div className="flex flex-wrap items-center gap-3 pt-1">
            <Button asChild size="lg" className="tracking-[0.1em]">
              <Link href={href}>Continuar estudando</Link>
            </Button>
            <Link
              href={href}
              className="text-sm text-muted-foreground transition-colors hover:text-primary"
            >
              Ver detalhes →
            </Link>
          </div>
        </div>
      </div>
    </article>
  );
}

export function NextLessonPanel({
  title,
  module,
  duration,
  href,
}: {
  title: string;
  module: string;
  duration: string;
  href: string;
}) {
  return (
    <aside className="rounded-md border border-border bg-surface p-4 sm:p-5">
      <p className="text-[0.625rem] font-medium tracking-[0.16em] text-primary uppercase">
        Próxima aula
      </p>
      <div className="relative mt-4 aspect-video overflow-hidden rounded-md bg-gradient-to-br from-navy-800 to-navy-950">
        <span className="absolute inset-0 flex items-center justify-center">
          <span className="flex size-11 items-center justify-center rounded-full border border-primary/50 bg-navy-950/60 text-primary">
            <Play className="size-4 fill-current" />
          </span>
        </span>
      </div>
      <h3 className="mt-4 font-heading text-lg font-medium tracking-[-0.015em]">{title}</h3>
      <p className="mt-1 text-sm text-muted-foreground">
        {module}
        {duration !== "—" ? ` · ${duration}` : ""}
      </p>
      <Button asChild variant="outline" className="mt-5 w-full">
        <Link href={href}>Assistir aula</Link>
      </Button>
    </aside>
  );
}
