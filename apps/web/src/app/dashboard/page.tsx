"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  ArrowRight,
  Archive,
  BookOpen,
  FileEdit,
  GraduationCap,
  ShieldCheck,
} from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { StatusBadge } from "@/components/status-badge";
import { useAuth } from "@/lib/auth-context";
import { apiFetch } from "@/lib/api-client";
import type { CourseSummary, PageResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";

function timeAgo(iso: string) {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 1) return "agora há pouco";
  if (minutes < 60) return `há ${minutes} min`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `há ${hours}h`;
  const days = Math.round(hours / 24);
  if (days < 30) return `há ${days}d`;
  return new Date(iso).toLocaleDateString("pt-BR");
}

function InstructorOverview({ isAdmin }: { isAdmin: boolean }) {
  const [courses, setCourses] = useState<CourseSummary[] | null>(null);
  const [userCount, setUserCount] = useState<number | null>(null);

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const page = await apiFetch<PageResponse<CourseSummary>>(
          "/courses?size=100&sort=updatedAt,desc",
        );
        if (active) setCourses(page.content);
      } catch {
        if (active) setCourses([]);
      }
      if (isAdmin) {
        try {
          const users = await apiFetch<PageResponse<unknown>>("/users?size=1");
          if (active) setUserCount(users.totalElements);
        } catch {
          if (active) setUserCount(null);
        }
      }
    }
    void load();
    return () => {
      active = false;
    };
  }, [isAdmin]);

  const stats = useMemo(() => {
    if (!courses) return null;
    const published = courses.filter((c) => c.status === "PUBLISHED").length;
    const draft = courses.filter((c) => c.status === "DRAFT").length;
    const archived = courses.filter((c) => c.status === "ARCHIVED").length;
    return { total: courses.length, published, draft, archived };
  }, [courses]);

  const needsAttention = useMemo(
    () => (courses ?? []).filter((c) => c.status === "DRAFT").slice(0, 4),
    [courses],
  );

  const recentActivity = useMemo(() => (courses ?? []).slice(0, 5), [courses]);

  if (!courses || !stats) {
    return (
      <div className="grid gap-4 lg:grid-cols-[1.3fr_1fr]">
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  const publishedRatio = stats.total === 0 ? 0 : Math.round((stats.published / stats.total) * 100);

  return (
    <div className="flex flex-col gap-8">
      <div className="grid gap-4 lg:grid-cols-[1.3fr_1fr]">
        <div className="flex flex-col justify-between gap-6 rounded-lg border border-border/70 bg-surface-elevated p-6 sm:p-7">
          <div className="flex items-start justify-between gap-4">
            <div>
              <span className="kicker">{isAdmin ? "Toda a plataforma" : "Seus cursos"}</span>
              <p className="mt-3 font-serif text-5xl leading-none font-medium tracking-tight">
                {stats.total}
              </p>
              <p className="mt-1.5 text-sm text-muted-foreground">
                {stats.total === 1 ? "curso cadastrado" : "cursos cadastrados"}
              </p>
            </div>
            <span className="flex size-10 shrink-0 items-center justify-center rounded-md bg-primary-soft text-primary-soft-foreground">
              <BookOpen className="size-5" />
            </span>
          </div>
          <div className="flex flex-col gap-2">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <span>{publishedRatio}% publicado</span>
              <span>
                {stats.published}/{stats.total}
              </span>
            </div>
            <Progress value={publishedRatio} />
          </div>
          <Link href="/courses">
            <Button variant="outline" size="sm" className="w-fit gap-1.5">
              Ver todos os cursos <ArrowRight className="size-3.5" />
            </Button>
          </Link>
        </div>

        <div className="grid grid-cols-3 gap-3 lg:grid-cols-1">
          <CompactStat label="Publicados" value={stats.published} />
          <CompactStat label="Rascunhos" value={stats.draft} />
          <CompactStat label="Arquivados" value={stats.archived} />
        </div>
      </div>

      <div className="grid gap-8 lg:grid-cols-2">
        <div className="flex flex-col gap-3">
          <div className="flex items-center justify-between">
            <h2 className="font-serif text-lg font-medium tracking-tight">Precisam de atenção</h2>
            <span className="text-xs text-muted-foreground">{needsAttention.length} em rascunho</span>
          </div>
          {needsAttention.length === 0 ? (
            <EmptyRow text="Nenhum curso em rascunho no momento." />
          ) : (
            <ul className="flex flex-col divide-y divide-border/70 rounded-lg border border-border/70">
              {needsAttention.map((course) => (
                <li key={course.id}>
                  <Link
                    href={`/courses/${course.id}`}
                    className="flex items-center justify-between gap-3 px-4 py-3 text-sm transition-colors hover:bg-muted/40"
                  >
                    <span className="min-w-0 truncate font-medium">{course.title}</span>
                    <StatusBadge status={course.status} />
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="flex flex-col gap-3">
          <h2 className="font-serif text-lg font-medium tracking-tight">Atividade recente</h2>
          {recentActivity.length === 0 ? (
            <EmptyRow text="Nenhuma atividade registrada ainda." />
          ) : (
            <ul className="flex flex-col divide-y divide-border/70 rounded-lg border border-border/70">
              {recentActivity.map((course) => (
                <li key={course.id}>
                  <Link
                    href={`/courses/${course.id}`}
                    className="flex items-center justify-between gap-3 px-4 py-3 text-sm transition-colors hover:bg-muted/40"
                  >
                    <span className="min-w-0 truncate">
                      <span className="font-medium">{course.title}</span>
                      <span className="text-muted-foreground"> · atualizado {timeAgo(course.updatedAt)}</span>
                    </span>
                    <StatusBadge status={course.status} />
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <div className="flex flex-col gap-3">
        <h2 className="font-serif text-lg font-medium tracking-tight text-muted-foreground">
          Em breve nas próximas fases
        </h2>
        <div className="grid gap-3 sm:grid-cols-3">
          <UpcomingStat icon={GraduationCap} label="Alunos e matrículas" />
          <UpcomingStat icon={ShieldCheck} label="Certificados" />
          <UpcomingStat icon={FileEdit} label="Exercícios do aluno" />
        </div>
        {isAdmin && userCount !== null && (
          <p className="text-xs text-muted-foreground">
            {userCount} conta{userCount === 1 ? "" : "s"} cadastrada{userCount === 1 ? "" : "s"} ·{" "}
            <Link href="/admin/users" className="font-medium text-primary hover:underline">
              gerenciar usuários
            </Link>
          </p>
        )}
      </div>
    </div>
  );
}

function CompactStat({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex flex-col justify-between gap-3 rounded-lg border border-border/70 bg-surface p-4">
      <p className="font-serif text-2xl leading-none font-medium tracking-tight">{value}</p>
      <p className="text-xs text-muted-foreground">{label}</p>
    </div>
  );
}

function UpcomingStat({
  icon: Icon,
  label,
}: {
  icon: React.ComponentType<{ className?: string }>;
  label: string;
}) {
  return (
    <div className="flex items-center gap-3 rounded-lg border border-dashed border-border/70 p-4 text-muted-foreground/70">
      <Icon className="size-4 shrink-0" />
      <div>
        <p className="text-sm font-medium">{label}</p>
        <p className="text-xs">Ainda não disponível</p>
      </div>
    </div>
  );
}

function EmptyRow({ text }: { text: string }) {
  return (
    <div className="rounded-lg border border-dashed border-border/70 px-4 py-8 text-center text-sm text-muted-foreground">
      {text}
    </div>
  );
}

function StudentOverview() {
  return (
    <div className="flex flex-col items-start gap-4 rounded-lg border border-dashed border-border/70 p-8">
      <span className="flex size-10 items-center justify-center rounded-md bg-primary-soft text-primary-soft-foreground">
        <GraduationCap className="size-5" />
      </span>
      <div className="flex flex-col gap-1">
        <h2 className="font-serif text-lg font-medium tracking-tight">Área do aluno chega na próxima fase</h2>
        <p className="max-w-md text-sm text-muted-foreground">
          Matrícula, progresso e player fazem parte da Fase 4. Por enquanto você não tem cursos
          matriculados.
        </p>
      </div>
    </div>
  );
}

function DashboardContent() {
  const { user } = useAuth();
  if (!user) return null;

  const isAdmin = user.roles.includes("SUPER_ADMIN");
  const isInstructor = user.roles.includes("INSTRUCTOR");
  const firstName = user.name.split(" ")[0];

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-8 p-6 sm:p-8">
      <div>
        <span className="kicker">Visão geral</span>
        <h1 className="mt-2 font-serif text-2xl font-medium tracking-tight sm:text-3xl">
          Olá, {firstName}
        </h1>
        <p className="text-muted-foreground">Dados reais da plataforma — sem métricas inventadas.</p>
      </div>

      {isAdmin || isInstructor ? (
        <InstructorOverview isAdmin={isAdmin} />
      ) : (
        <StudentOverview />
      )}
    </div>
  );
}

export default function DashboardPage() {
  return (
    <ProtectedRoute>
      <DashboardShell>
        <DashboardContent />
      </DashboardShell>
    </ProtectedRoute>
  );
}
