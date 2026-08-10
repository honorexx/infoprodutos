"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { ApiImage } from "@/components/ui/api-image";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { apiFetch, ApiError } from "@/lib/api-client";
import { startCheckout } from "@/lib/checkout";
import type { CourseSummary, Enrollment, ProductPackage } from "@/lib/types";
import { formatBrlFromCents } from "@/lib/utils";

function DiscoverContent() {
  const [courses, setCourses] = useState<CourseSummary[] | null>(null);
  const [packages, setPackages] = useState<ProductPackage[]>([]);
  const [enrolledIds, setEnrolledIds] = useState<Set<string>>(new Set());
  const [buyingId, setBuyingId] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const { syncPendingPurchases } = await import("@/lib/sync-pending-purchases");
        const unlocked = await syncPendingPurchases();
        if (!cancelled && unlocked.length > 0) {
          toast.success(
            unlocked.length === 1
              ? "Pagamento confirmado — curso liberado."
              : `${unlocked.length} compras liberadas.`,
          );
        }
        const [catalog, pkgs, enrollments] = await Promise.all([
          apiFetch<CourseSummary[]>("/catalog/courses", { skipAuth: true }),
          apiFetch<ProductPackage[]>("/catalog/packages", { skipAuth: true }),
          apiFetch<Enrollment[]>("/enrollments/me").catch(() => [] as Enrollment[]),
        ]);
        if (cancelled) return;
        setCourses(catalog.filter((c) => (c.priceCents ?? 0) > 0));
        setPackages(pkgs);
        setEnrolledIds(
          new Set(enrollments.filter((e) => e.status === "ACTIVE").map((e) => e.courseId)),
        );
      } catch (error) {
        if (!cancelled) {
          toast.error(
            error instanceof ApiError
              ? (error.body?.detail ?? error.message)
              : "Não foi possível carregar o catálogo.",
          );
          setCourses([]);
        }
      }
    }
    void load();
    return () => {
      cancelled = true;
    };
  }, []);

  async function buyCourse(courseId: string) {
    setBuyingId(courseId);
    try {
      await startCheckout({ courseId });
    } catch (error) {
      toast.error(
        error instanceof ApiError
          ? (error.body?.detail ?? error.message)
          : "Falha ao iniciar a compra.",
      );
    } finally {
      setBuyingId(null);
    }
  }

  async function buyPackage(packageId: string) {
    setBuyingId(`pkg-${packageId}`);
    try {
      await startCheckout({ packageId });
    } catch (error) {
      toast.error(
        error instanceof ApiError
          ? (error.body?.detail ?? error.message)
          : "Falha ao iniciar a compra.",
      );
    } finally {
      setBuyingId(null);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-8 p-4 sm:p-6 lg:p-8">
      <div>
        <span className="kicker">Catálogo</span>
        <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight sm:text-3xl">
          Descobrir
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Todos os cursos publicados. Compre o que quiser — cada um libera o próprio acesso.
        </p>
      </div>

      {packages.length > 0 && (
        <section className="flex flex-col gap-4">
          <h2 className="font-heading text-lg font-medium tracking-tight">Pacotes</h2>
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {packages.map((pkg) => (
              <li
                key={pkg.id}
                className="flex flex-col overflow-hidden rounded-md border border-border bg-surface"
              >
                <div className="relative aspect-[16/10] bg-navy-900">
                  <ApiImage
                    src={pkg.courses[0]?.coverImageUrl}
                    alt=""
                    className="absolute inset-0 size-full object-cover"
                    fallbackClassName="absolute inset-0"
                  />
                </div>
                <div className="flex flex-1 flex-col gap-2 p-4">
                  <Badge variant="gold" className="w-fit">
                    Pacote · {pkg.courses.length} cursos
                  </Badge>
                  <h3 className="font-heading text-base font-medium tracking-tight">{pkg.title}</h3>
                  <p className="text-xs text-muted-foreground line-clamp-2">
                    {pkg.courses.map((c) => c.title).join(" · ")}
                  </p>
                  <p className="mt-auto pt-2 font-medium text-primary">
                    {formatBrlFromCents(pkg.priceCents)}
                  </p>
                  <Button
                    size="sm"
                    disabled={buyingId === `pkg-${pkg.id}`}
                    onClick={() => void buyPackage(pkg.id)}
                  >
                    {buyingId === `pkg-${pkg.id}` ? "Abrindo…" : "Comprar pacote"}
                  </Button>
                </div>
              </li>
            ))}
          </ul>
        </section>
      )}

      <section className="flex flex-col gap-4">
        <h2 className="font-heading text-lg font-medium tracking-tight">Cursos publicados</h2>
        {courses === null ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Skeleton className="h-64 w-full" />
            <Skeleton className="h-64 w-full" />
            <Skeleton className="h-64 w-full" />
          </div>
        ) : courses.length === 0 ? (
          <p className="rounded-md border border-dashed border-border p-8 text-sm text-muted-foreground">
            Nenhum curso publicado com preço no momento.
          </p>
        ) : (
          <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {courses.map((course) => {
              const owned = enrolledIds.has(course.id);
              return (
                <li
                  key={course.id}
                  className="flex flex-col overflow-hidden rounded-md border border-border bg-surface"
                >
                  <div className="relative aspect-[16/10] bg-navy-900">
                    <ApiImage
                      src={course.coverImageUrl}
                      alt=""
                      className="absolute inset-0 size-full object-cover"
                      fallbackClassName="absolute inset-0"
                    />
                  </div>
                  <div className="flex flex-1 flex-col gap-2 p-4">
                    <div className="flex flex-wrap items-center gap-2">
                      <Badge variant={owned ? "gold" : "outline"} className="w-fit">
                        {owned ? "Já liberado" : "Disponível"}
                      </Badge>
                      {course.workloadHours != null && (
                        <span className="text-[11px] text-muted-foreground">
                          {course.workloadHours}h
                        </span>
                      )}
                    </div>
                    <h3 className="font-heading text-base font-medium tracking-tight">
                      {course.title}
                    </h3>
                    <p className="mt-auto pt-2 font-medium text-primary">
                      {formatBrlFromCents(course.priceCents)}
                    </p>
                    {owned ? (
                      <Button asChild size="sm" variant="outline">
                        <Link href={`/my-courses/${course.id}`}>Continuar</Link>
                      </Button>
                    ) : (
                      <Button
                        size="sm"
                        disabled={buyingId === course.id}
                        onClick={() => void buyCourse(course.id)}
                      >
                        {buyingId === course.id ? "Abrindo…" : "Comprar"}
                      </Button>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </div>
  );
}

export default function DiscoverPage() {
  return (
    <ProtectedRoute>
      <DashboardShell>
        <DiscoverContent />
      </DashboardShell>
    </ProtectedRoute>
  );
}
