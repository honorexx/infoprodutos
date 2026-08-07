"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowRight, BookOpen } from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { apiFetch } from "@/lib/api-client";
import type { Enrollment, ProgressSummary } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";

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
                /* progresso opcional na listagem */
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
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-4 p-6 sm:p-8">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-28 w-full" />
        <Skeleton className="h-28 w-full" />
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-8 p-6 sm:p-8">
      <div>
        <span className="kicker">Área do aluno</span>
        <h1 className="mt-2 font-serif text-2xl font-medium tracking-tight sm:text-3xl">Meus cursos</h1>
        <p className="text-muted-foreground">Cursos com matrícula concedida por um professor ou admin.</p>
      </div>

      {enrollments.length === 0 ? (
        <div className="flex flex-col items-start gap-3 rounded-lg border border-dashed border-border/70 p-8">
          <BookOpen className="size-5 text-muted-foreground" />
          <p className="text-sm text-muted-foreground">
            Você ainda não tem matrículas. Peça ao professor para liberar o acesso.
          </p>
        </div>
      ) : (
        <ul className="flex flex-col gap-3">
          {enrollments.map((enrollment) => {
            const summary = progressById[enrollment.id];
            const pct = summary?.courseCompletionPercent ?? 0;
            return (
              <li
                key={enrollment.id}
                className="flex flex-col gap-4 rounded-lg border border-border/70 bg-surface-elevated p-5 sm:flex-row sm:items-center sm:justify-between"
              >
                <div className="flex-1 space-y-2">
                  <div className="flex flex-wrap items-center gap-2">
                    <h2 className="font-serif text-lg font-medium tracking-tight">
                      {enrollment.courseTitle}
                    </h2>
                    <Badge variant="outline">{STATUS_LABEL[enrollment.status] ?? enrollment.status}</Badge>
                  </div>
                  {enrollment.status === "ACTIVE" && (
                    <div className="max-w-sm space-y-1">
                      <div className="flex justify-between text-xs text-muted-foreground">
                        <span>Progresso</span>
                        <span>
                          {summary
                            ? `${summary.completedLessons}/${summary.totalPublishedLessons} aulas`
                            : "—"}
                        </span>
                      </div>
                      <Progress value={pct} />
                    </div>
                  )}
                </div>
                {enrollment.status === "ACTIVE" ? (
                  <Button asChild>
                    <Link href={`/my-courses/${enrollment.courseId}?enrollmentId=${enrollment.id}`}>
                      Continuar
                      <ArrowRight className="size-4" />
                    </Link>
                  </Button>
                ) : (
                  <p className="text-xs text-muted-foreground">Acesso indisponível</p>
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
