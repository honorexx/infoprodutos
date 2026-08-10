"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useAuth } from "@/lib/auth-context";
import { apiFetch } from "@/lib/api-client";
import type { CourseSummary, Enrollment, ProductPackage, ProgressSummary } from "@/lib/types";
import { loginUrlForCheckout } from "@/lib/checkout";
import { formatBrlFromCents, cn } from "@/lib/utils";
import { GoldExpandLine, Reveal, Stagger, StaggerItem } from "@/components/landing/reveal";
import { ApiImage } from "@/components/ui/api-image";

type FormationCard = {
  id: string;
  title: string;
  category: string;
  workloadHours: number | null;
  moduleHint: string | null;
  coverImageUrl: string | null;
  progress: number | null;
  href: string;
  priceCents: number | null;
  buyHref: string | null;
  featured?: boolean;
  kind: "course" | "package" | "enrollment";
};

function toShopCourse(c: CourseSummary, loggedIn: boolean, featured: boolean): FormationCard {
  const buyHref = loggedIn
    ? `/checkout?courseId=${c.id}`
    : loginUrlForCheckout({ courseId: c.id });
  return {
    id: c.id,
    title: c.title,
    category: "Curso",
    workloadHours: c.workloadHours,
    moduleHint: null,
    coverImageUrl: c.coverImageUrl,
    progress: null,
    href: buyHref,
    priceCents: c.priceCents,
    buyHref,
    featured,
    kind: "course",
  };
}

function toShopPackage(p: ProductPackage, loggedIn: boolean, featured: boolean): FormationCard {
  const buyHref = loggedIn
    ? `/checkout?packageId=${p.id}`
    : loginUrlForCheckout({ packageId: p.id });
  return {
    id: `pkg-${p.id}`,
    title: p.title,
    category: "Pacote",
    workloadHours: null,
    moduleHint: `${p.courses.length} cursos`,
    coverImageUrl: p.courses[0]?.coverImageUrl ?? null,
    progress: null,
    href: buyHref,
    priceCents: p.priceCents,
    buyHref,
    featured,
    kind: "package",
  };
}

export function HomeFormations() {
  const { user } = useAuth();
  const [cards, setCards] = useState<FormationCard[] | null>(null);

  useEffect(() => {
    let active = true;

    async function load() {
      try {
        const [catalog, packages] = await Promise.all([
          apiFetch<CourseSummary[]>("/catalog/courses", { skipAuth: true }),
          apiFetch<ProductPackage[]>("/catalog/packages", { skipAuth: true }),
        ]);

        const published = catalog.filter((c) => (c.priceCents ?? 0) > 0);
        const loggedIn = Boolean(user);

        if (user?.roles.includes("STUDENT")
          && !user.roles.includes("INSTRUCTOR")
          && !user.roles.includes("SUPER_ADMIN")) {
          const enrollments = await apiFetch<Enrollment[]>("/enrollments/me");
          const activeEnrollments = enrollments.filter((e) => e.status === "ACTIVE");
          const enrolledIds = new Set(activeEnrollments.map((e) => e.courseId));

          const progressMap: Record<string, ProgressSummary> = {};
          await Promise.all(
            activeEnrollments.map(async (e) => {
              try {
                progressMap[e.id] = await apiFetch<ProgressSummary>(
                  `/enrollments/${e.id}/progress/summary`,
                );
              } catch {
                /* opcional */
              }
            }),
          );

          if (!active) return;

          const enrolledCards: FormationCard[] = activeEnrollments.map((e, i) => {
            const summary = progressMap[e.id];
            return {
              id: e.id,
              title: e.courseTitle,
              category: "Seu acesso",
              workloadHours: null,
              moduleHint: summary ? `${summary.modules.length} módulos` : null,
              coverImageUrl: e.courseCoverImageUrl ?? null,
              progress: summary?.courseCompletionPercent ?? null,
              href: `/my-courses/${e.courseId}`,
              priceCents: null,
              buyHref: null,
              featured: i === 0,
              kind: "enrollment",
            };
          });

          const shopCards: FormationCard[] = [
            ...packages.map((p, i) =>
              toShopPackage(p, true, enrolledCards.length === 0 && i === 0),
            ),
            ...published
              .filter((c) => !enrolledIds.has(c.id))
              .map((c, i) =>
                toShopCourse(
                  c,
                  true,
                  enrolledCards.length === 0 && packages.length === 0 && i === 0,
                ),
              ),
          ];

          setCards([...enrolledCards, ...shopCards]);
          return;
        }

        if (!active) return;
        const shop: FormationCard[] = [
          ...packages.map((p, i) => toShopPackage(p, loggedIn, i === 0)),
          ...published.map((c, i) =>
            toShopCourse(c, loggedIn, packages.length === 0 && i === 0),
          ),
        ];
        setCards(shop);
      } catch {
        if (active) setCards([]);
      }
    }

    void load();
    return () => {
      active = false;
    };
  }, [user]);

  const list = cards ?? [];
  const featured = list.find((c) => c.featured) ?? list[0];
  const secondary = list.filter((c) => c.id !== featured?.id).slice(0, 6);

  return (
    <section id="formacoes" className="scroll-mt-20 border-y border-white/[0.04] bg-navy-850">
      <div className="mx-auto max-w-7xl px-5 py-24 sm:px-8 lg:px-10 lg:py-32">
        <div className="flex flex-wrap items-end justify-between gap-6">
          <div className="max-w-2xl">
            <Reveal>
              <p className="text-[0.6875rem] font-medium tracking-[0.22em] text-primary uppercase">
                Formações
              </p>
            </Reveal>
            <Reveal delay={0.05}>
              <h2 className="mt-6 text-balance font-heading text-3xl leading-[1.12] font-medium tracking-[-0.02em] text-foreground sm:text-4xl lg:text-[2.65rem]">
                Cursos publicados.
                <br />
                Escolha e libere o acesso.
              </h2>
            </Reveal>
            <Reveal delay={0.1}>
              <GoldExpandLine className="mt-8 w-12" />
            </Reveal>
          </div>
          <Reveal delay={0.12}>
            <Link
              href="/cursos"
              className="text-[0.6875rem] font-semibold tracking-[0.14em] text-primary uppercase transition-colors hover:text-primary-hover"
            >
              Ver todos →
            </Link>
          </Reveal>
        </div>

        {cards === null ? (
          <div className="mt-16 grid gap-5 lg:grid-cols-[1.35fr_1fr]">
            <div className="h-80 animate-pulse bg-navy-800" />
            <div className="flex flex-col gap-5">
              <div className="h-36 animate-pulse bg-navy-800" />
              <div className="h-36 animate-pulse bg-navy-800" />
            </div>
          </div>
        ) : list.length === 0 ? (
          <p className="mt-16 text-sm text-muted-foreground">
            Nenhum curso publicado com preço definido ainda.
          </p>
        ) : (
          <Stagger className="mt-16 grid gap-5 lg:grid-cols-[1.35fr_1fr] lg:gap-6">
            {featured && (
              <StaggerItem>
                <FormationCardView card={featured} size="lg" />
              </StaggerItem>
            )}
            <StaggerItem>
              <div className="flex h-full flex-col gap-5">
                {secondary.length > 0 ? (
                  secondary.map((card) => (
                    <FormationCardView key={card.id} card={card} size="sm" />
                  ))
                ) : (
                  <p className="text-sm text-muted-foreground">
                    Publique mais cursos para preencher a vitrine.
                  </p>
                )}
              </div>
            </StaggerItem>
          </Stagger>
        )}
      </div>
    </section>
  );
}

function FormationCardView({
  card,
  size,
}: {
  card: FormationCard;
  size: "lg" | "sm";
}) {
  const cta = card.buyHref
    ? card.kind === "package"
      ? "Comprar pacote"
      : "Comprar curso"
    : card.progress != null
      ? "Continuar"
      : "Acessar";

  return (
    <Link
      href={card.buyHref ?? card.href}
      className={cn(
        "group relative flex overflow-hidden border border-white/[0.07] bg-navy-800 transition-[border-color,transform] duration-300 hover:border-[rgba(186,147,100,0.35)]",
        size === "lg" ? "min-h-[340px] flex-col" : "min-h-[140px] flex-row",
      )}
    >
      <div
        className={cn(
          "relative overflow-hidden bg-navy-700",
          size === "lg" ? "h-48 w-full sm:h-56" : "w-[38%] shrink-0 self-stretch",
        )}
      >
        <ApiImage
          src={card.coverImageUrl}
          alt=""
          className="absolute inset-0 size-full object-cover transition-transform duration-500 group-hover:scale-[1.02]"
          fallbackClassName="absolute inset-0"
        />
        {!card.coverImageUrl && (
          <div
            aria-hidden
            className="absolute inset-0 transition-transform duration-500 group-hover:scale-[1.02]"
            style={{
              background:
                "linear-gradient(145deg, rgba(186,147,100,0.18) 0%, transparent 50%), #192538",
            }}
          />
        )}
      </div>
      <div
        className={cn(
          "relative flex flex-1 flex-col justify-end p-5 sm:p-6",
          size === "lg" && "p-7 sm:p-8",
        )}
      >
        <p className="text-[0.625rem] font-medium tracking-[0.16em] text-primary uppercase">
          {card.category}
        </p>
        <h3
          className={cn(
            "mt-2 font-heading font-medium tracking-[-0.015em] text-foreground transition-transform duration-300 group-hover:translate-y-[-2px]",
            size === "lg" ? "text-2xl sm:text-[1.75rem]" : "text-lg",
          )}
        >
          {card.title}
        </h3>
        <div className="mt-4 flex flex-wrap items-center gap-x-5 gap-y-1 text-[0.75rem] tracking-[0.04em] text-slate-400">
          {card.priceCents != null && card.priceCents > 0 && (
            <span className="font-medium text-primary">{formatBrlFromCents(card.priceCents)}</span>
          )}
          {card.workloadHours != null && <span>{card.workloadHours}h</span>}
          {card.moduleHint && <span>{card.moduleHint}</span>}
          {card.progress != null && <span>{Math.round(card.progress)}% concluído</span>}
        </div>
        <span className="mt-4 text-[0.6875rem] font-semibold tracking-[0.14em] text-primary uppercase">
          {cta} →
        </span>
      </div>
    </Link>
  );
}
