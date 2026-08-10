"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { SiteHeader } from "@/components/site-header";
import { HomeFooter } from "@/components/landing/home-footer";
import { ApiImage } from "@/components/ui/api-image";
import { apiFetch } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";
import { loginUrlForCheckout } from "@/lib/checkout";
import type { CourseSummary, Enrollment, ProductPackage } from "@/lib/types";
import { formatBrlFromCents } from "@/lib/utils";
import { Skeleton } from "@/components/ui/skeleton";

/**
 * Vitrine pública: qualquer visitante vê cursos publicados + pacotes com preço.
 * Compra exige login; conteúdo das aulas só com matrícula.
 */
export default function PublicCoursesPage() {
  const { user } = useAuth();
  const [courses, setCourses] = useState<CourseSummary[] | null>(null);
  const [packages, setPackages] = useState<ProductPackage[]>([]);
  const [enrolledIds, setEnrolledIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const [catalog, pkgs] = await Promise.all([
          apiFetch<CourseSummary[]>("/catalog/courses", { skipAuth: true }),
          apiFetch<ProductPackage[]>("/catalog/packages", { skipAuth: true }),
        ]);
        if (cancelled) return;
        setCourses(catalog.filter((c) => (c.priceCents ?? 0) > 0));
        setPackages(pkgs);

        if (user) {
          try {
            const enrollments = await apiFetch<Enrollment[]>("/enrollments/me");
            if (!cancelled) {
              setEnrolledIds(
                new Set(
                  enrollments.filter((e) => e.status === "ACTIVE").map((e) => e.courseId),
                ),
              );
            }
          } catch {
            /* visitante autenticado sem matrículas */
          }
        }
      } catch {
        if (!cancelled) {
          setCourses([]);
          setPackages([]);
        }
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, [user]);

  return (
    <div className="min-h-screen bg-navy-950 text-foreground">
      <SiteHeader />
      <main className="mx-auto max-w-7xl px-5 pt-24 pb-20 sm:px-8 lg:px-10">
        <p className="text-[0.6875rem] font-medium tracking-[0.22em] text-primary uppercase">
          Catálogo
        </p>
        <h1 className="mt-4 font-heading text-3xl font-medium tracking-[-0.02em] sm:text-4xl">
          Cursos publicados
        </h1>
        <p className="mt-3 max-w-xl text-sm text-muted-foreground">
          Qualquer pessoa pode ver a vitrine. Para liberar o conteúdo de um curso, é preciso
          comprar aquele curso (ou um pacote que o inclua).
        </p>

        {packages.length > 0 && (
          <section className="mt-14">
            <h2 className="font-heading text-xl font-medium tracking-tight">Pacotes</h2>
            <ul className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {packages.map((pkg) => {
                const href = user
                  ? `/checkout?packageId=${pkg.id}`
                  : loginUrlForCheckout({ packageId: pkg.id });
                return (
                  <li key={pkg.id}>
                    <Link
                      href={href}
                      className="group flex h-full flex-col overflow-hidden border border-white/[0.07] bg-navy-900 transition-colors hover:border-[rgba(186,147,100,0.35)]"
                    >
                      <div className="relative aspect-[16/10] bg-navy-800">
                        <ApiImage
                          src={pkg.courses[0]?.coverImageUrl}
                          alt=""
                          className="absolute inset-0 size-full object-cover"
                          fallbackClassName="absolute inset-0"
                        />
                      </div>
                      <div className="flex flex-1 flex-col gap-2 p-4">
                        <p className="text-[0.625rem] font-medium tracking-[0.16em] text-primary uppercase">
                          Pacote · {pkg.courses.length} cursos
                        </p>
                        <h3 className="font-heading text-lg font-medium tracking-tight">
                          {pkg.title}
                        </h3>
                        <p className="mt-auto font-medium text-primary">
                          {formatBrlFromCents(pkg.priceCents)}
                        </p>
                        <span className="text-[0.6875rem] font-semibold tracking-[0.14em] text-primary uppercase">
                          Comprar →
                        </span>
                      </div>
                    </Link>
                  </li>
                );
              })}
            </ul>
          </section>
        )}

        <section className="mt-14">
          <h2 className="font-heading text-xl font-medium tracking-tight">Cursos</h2>
          {courses === null ? (
            <div className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <Skeleton className="h-64 w-full" />
              <Skeleton className="h-64 w-full" />
              <Skeleton className="h-64 w-full" />
            </div>
          ) : courses.length === 0 ? (
            <p className="mt-5 text-sm text-muted-foreground">
              Nenhum curso publicado com preço no momento.
            </p>
          ) : (
            <ul className="mt-5 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {courses.map((course) => {
                const owned = enrolledIds.has(course.id);
                const href = owned
                  ? `/my-courses/${course.id}`
                  : user
                    ? `/checkout?courseId=${course.id}`
                    : loginUrlForCheckout({ courseId: course.id });
                return (
                  <li key={course.id}>
                    <Link
                      href={href}
                      className="group flex h-full flex-col overflow-hidden border border-white/[0.07] bg-navy-900 transition-colors hover:border-[rgba(186,147,100,0.35)]"
                    >
                      <div className="relative aspect-[16/10] bg-navy-800">
                        <ApiImage
                          src={course.coverImageUrl}
                          alt=""
                          className="absolute inset-0 size-full object-cover"
                          fallbackClassName="absolute inset-0"
                        />
                      </div>
                      <div className="flex flex-1 flex-col gap-2 p-4">
                        <p className="text-[0.625rem] font-medium tracking-[0.16em] text-primary uppercase">
                          {owned ? "Seu acesso" : "Curso"}
                        </p>
                        <h3 className="font-heading text-lg font-medium tracking-tight">
                          {course.title}
                        </h3>
                        {course.workloadHours != null && (
                          <p className="text-xs text-muted-foreground">{course.workloadHours}h</p>
                        )}
                        <p className="mt-auto font-medium text-primary">
                          {owned ? "Já liberado" : formatBrlFromCents(course.priceCents)}
                        </p>
                        <span className="text-[0.6875rem] font-semibold tracking-[0.14em] text-primary uppercase">
                          {owned ? "Continuar →" : "Comprar →"}
                        </span>
                      </div>
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </main>
      <HomeFooter />
    </div>
  );
}
