"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { BookOpen } from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { apiFetch } from "@/lib/api-client";
import type { Enrollment, ProgressSummary } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { CourseCardCompact } from "@/components/dashboard/student-panels";

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "Ativa",
  SUSPENDED: "Suspensa",
  CANCELLED: "Cancelada",
  EXPIRED: "Expirada",
};

function MyCoursesContent() {
  const [enrollments, setEnrollments] = useState<Enrollment[] | null>(null);
  const [progressById, setProgressById] = useState<Record<string, ProgressSummary>>({});

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const { syncPendingPurchases } = await import("@/lib/sync-pending-purchases");
        const unlocked = await syncPendingPurchases();
        if (unlocked.length > 0 && active) {
          const { toast } = await import("sonner");
          toast.success(
            unlocked.length === 1
              ? "Pagamento confirmado — curso liberado."
              : `${unlocked.length} compras liberadas.`,
          );
        }
        const list = await apiFetch<Enrollment[]>("/enrollments/me");
        if (!active) return;
        setEnrollments(list);
        const summaries: Record<string, ProgressSummary> = {};
        await Promise.all(
          list
            .filter((e) => e.status === "ACTIVE")
            .map(async (e) => {
              try {
                summaries[e.id] = await apiFetch<ProgressSummary>(
                  `/enrollments/${e.id}/progress/summary`,
                );
              } catch {
                /* opcional */
              }
            }),
        );
        if (active) setProgressById(summaries);
      } catch {
        if (active) setEnrollments([]);
      }
    }
    void load();
    return () => {
      active = false;
    };
  }, []);

  if (!enrollments) {
    return (
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-4 p-6 sm:p-8">
        <Skeleton className="h-10 w-48" />
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Skeleton className="h-52 w-full" />
          <Skeleton className="h-52 w-full" />
          <Skeleton className="h-52 w-full" />
        </div>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-8 p-6 sm:p-8">
      <div>
        <span className="kicker">Área do aluno</span>
        <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight sm:text-3xl">
          Meus cursos
        </h1>
        <p className="mt-1 text-muted-foreground">
          Formações com matrícula ativa. Continue de onde parou.
        </p>
      </div>

      {enrollments.length === 0 ? (
        <div className="flex flex-col items-start gap-3 rounded-md border border-dashed border-border p-8">
          <BookOpen className="size-5 text-primary" />
          <p className="text-sm text-muted-foreground">
            Você ainda não tem matrículas. Peça ao professor para liberar o acesso.
          </p>
          <Button asChild variant="outline">
            <Link href="/dashboard">Voltar ao dashboard</Link>
          </Button>
        </div>
      ) : (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {enrollments.map((enrollment, i) => {
            const summary = progressById[enrollment.id];
            const pct = summary?.courseCompletionPercent ?? 0;
            const tones = ["navy", "gold", "slate"] as const;
            return (
              <li key={enrollment.id} className="flex flex-col gap-2">
                <CourseCardCompact
                  title={enrollment.courseTitle}
                  subtitle={STATUS_LABEL[enrollment.status] ?? enrollment.status}
                  progress={pct}
                  href={`/my-courses/${enrollment.courseId}`}
                  coverImageUrl={enrollment.courseCoverImageUrl}
                  coverTone={tones[i % 3]}
                />
                {enrollment.status === "ACTIVE" && (
                  <Badge variant="gold" className="w-fit">
                    {Math.round(pct)}% · Continuar
                  </Badge>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

export default function MyCoursesPage() {
  return (
    <ProtectedRoute>
      <DashboardShell>
        <MyCoursesContent />
      </DashboardShell>
    </ProtectedRoute>
  );
}
