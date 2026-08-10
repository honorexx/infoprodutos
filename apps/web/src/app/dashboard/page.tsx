"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import {
  ArrowRight,
  BookOpen,
  GraduationCap,
  TrendingUp,
  Users,
} from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { useAuth } from "@/lib/auth-context";
import { apiFetch } from "@/lib/api-client";
import type { DashboardStats, Enrollment, ProgressSummary } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { ProgressRing } from "@/components/dashboard/progress-ring";
import {
  CourseCardCompact,
  CurrentCoursePanel,
  NextLessonPanel,
} from "@/components/dashboard/student-panels";

const STUDENT_QUOTE = "Disciplina hoje, liberdade amanhã.";

function formatLessonDuration(seconds: number | null | undefined) {
  if (seconds == null || seconds <= 0) return "—";
  const minutes = Math.max(1, Math.round(seconds / 60));
  return `${minutes} min`;
}

function formatStudyHours(totalSeconds: number) {
  if (totalSeconds <= 0) return "0h";
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.round((totalSeconds % 3600) / 60);
  if (hours === 0) return `${minutes}min`;
  if (minutes === 0) return `${hours}h`;
  return `${hours}h ${minutes}min`;
}
function LearningChart({
  year,
  series,
  caption,
}: {
  year: number;
  series: { label: string; month: number; value: number }[];
  caption: string;
}) {
  const width = 640;
  const height = 220;
  const padX = 28;
  const padY = 24;
  const maxY = 100;

  const points = useMemo(() => {
    if (!series.length) return [];
    const innerW = width - padX * 2;
    const innerH = height - padY * 2;
    return series.map((p, i) => {
      const x = padX + (series.length === 1 ? innerW / 2 : (i / (series.length - 1)) * innerW);
      const y = padY + innerH - (Math.min(maxY, Math.max(0, p.value)) / maxY) * innerH;
      return { ...p, x, y };
    });
  }, [series]);

  const line = points.map((p, i) => `${i === 0 ? "M" : "L"} ${p.x} ${p.y}`).join(" ");
  const area =
    points.length > 0
      ? `${line} L ${points[points.length - 1].x} ${height - padY} L ${points[0].x} ${height - padY} Z`
      : "";

  return (
    <div className="flex flex-col gap-4 rounded-md border border-border bg-surface p-5 sm:p-6">
      <div>
        <span className="kicker">Ritmo · {year}</span>
        <h2 className="mt-2 font-heading text-lg font-medium tracking-tight">Evolução no ano</h2>
        <p className="mt-1 max-w-md text-sm text-muted-foreground">{caption}</p>
      </div>
      <div className="w-full overflow-x-auto">
        <svg
          viewBox={`0 0 ${width} ${height}`}
          className="h-auto min-w-[320px] w-full"
          role="img"
          aria-label={`Gráfico de aprendizado ${year}`}
        >
          {[0, 25, 50, 75, 100].map((tick) => {
            const y = padY + (height - padY * 2) * (1 - tick / maxY);
            return (
              <g key={tick}>
                <line x1={padX} x2={width - padX} y1={y} y2={y} className="stroke-border" strokeWidth={1} />
                <text x={8} y={y + 3} className="fill-muted-foreground text-[9px]">
                  {tick}
                </text>
              </g>
            );
          })}
          {area && <path d={area} className="fill-primary/10" />}
          {line && (
            <path d={line} className="stroke-primary fill-none" strokeWidth={2} strokeLinejoin="round" />
          )}
          {points.map((p) => (
            <circle key={p.month} cx={p.x} cy={p.y} r={3} className="fill-primary" />
          ))}
          {points.map((p) => (
            <text
              key={`l-${p.month}`}
              x={p.x}
              y={height - 6}
              textAnchor="middle"
              className="fill-muted-foreground text-[10px]"
            >
              {p.label}
            </text>
          ))}
        </svg>
      </div>
    </div>
  );
}

function MetricCard({
  label,
  value,
  hint,
  icon: Icon,
  featured,
}: {
  label: string;
  value: string | number;
  hint?: string;
  icon: React.ComponentType<{ className?: string }>;
  featured?: boolean;
}) {
  return (
    <div
      className={cn(
        "flex flex-col gap-3 rounded-md border border-border bg-surface p-4 sm:p-5",
        featured && "border-border-gold sm:col-span-2 lg:col-span-1",
      )}
    >
      <div className="flex items-center justify-between gap-2">
        <p className="text-xs text-muted-foreground">{label}</p>
        <Icon className="size-4 text-primary" />
      </div>
      <p className="font-heading text-3xl leading-none font-medium tracking-tight">{value}</p>
      {hint ? <p className="text-xs text-muted-foreground">{hint}</p> : null}
    </div>
  );
}

function StudentExperience({
  stats,
  firstName,
}: {
  stats: DashboardStats | null;
  firstName: string;
}) {
  const [enrollments, setEnrollments] = useState<Enrollment[] | null>(null);
  const [progressById, setProgressById] = useState<Record<string, ProgressSummary>>({});

  useEffect(() => {
    let active = true;
    async function load() {
      try {
        const list = await apiFetch<Enrollment[]>("/enrollments/me");
        if (!active) return;
        setEnrollments(list);
        const map: Record<string, ProgressSummary> = {};
        await Promise.all(
          list
            .filter((e) => e.status === "ACTIVE")
            .slice(0, 8)
            .map(async (e) => {
              try {
                map[e.id] = await apiFetch<ProgressSummary>(`/enrollments/${e.id}/progress/summary`);
              } catch {
                /* progresso opcional por matrícula */
              }
            }),
        );
        if (active) setProgressById(map);
      } catch {
        if (active) setEnrollments([]);
      }
    }
    void load();
    return () => {
      active = false;
    };
  }, []);

  const active = (enrollments ?? []).filter((e) => e.status === "ACTIVE");
  const primary = active[0];
  const primaryProgress = primary ? progressById[primary.id] : undefined;

  const nextFromProgress = (() => {
    if (!primaryProgress) return null;
    for (const mod of primaryProgress.modules) {
      const lesson = mod.lessons.find((l) => l.progressStatus !== "COMPLETED");
      if (lesson) {
        return {
          title: lesson.title,
          module: mod.moduleTitle,
          duration: formatLessonDuration(lesson.durationSeconds),
          href: `/my-courses/${primary!.courseId}`,
        };
      }
    }
    return null;
  })();

  const current = primary
    ? {
        title: primary.courseTitle,
        category: primaryProgress
          ? `${primaryProgress.completedLessons} de ${primaryProgress.totalPublishedLessons} aulas`
          : "Formação",
        progress: primaryProgress?.courseCompletionPercent ?? 0,
        nextLesson: nextFromProgress?.title ?? "Continuar de onde parou",
        href: `/my-courses/${primary.courseId}`,
        coverImageUrl:
          primaryProgress?.courseCoverImageUrl ?? primary.courseCoverImageUrl ?? null,
      }
    : null;

  const courseCards = active.slice(0, 4).map((e, i) => {
    const summary = progressById[e.id];
    return {
      id: e.id,
      title: e.courseTitle,
      subtitle: summary
        ? `${summary.completedLessons}/${summary.totalPublishedLessons} aulas`
        : e.status === "ACTIVE"
          ? "Matriculado"
          : e.status,
      progress: summary?.courseCompletionPercent ?? 0,
      href: `/my-courses/${e.courseId}`,
      coverImageUrl: summary?.courseCoverImageUrl ?? e.courseCoverImageUrl ?? null,
      coverTone: (["navy", "gold", "slate", "navy"] as const)[i % 4],
    };
  });

  const lessonsCompleted = Object.values(progressById).reduce(
    (sum, s) => sum + (s.completedLessons ?? 0),
    0,
  );
  const studySeconds = Object.values(progressById).reduce((sum, s) => {
    return (
      sum +
      s.modules.reduce(
        (mSum, mod) =>
          mSum +
          mod.lessons
            .filter((l) => l.progressStatus === "COMPLETED")
            .reduce((lSum, l) => lSum + (l.durationSeconds ?? 0), 0),
        0,
      )
    );
  }, 0);

  const overallPercentFromProgress =
    Object.values(progressById).length > 0
      ? Math.round(
          (Object.values(progressById).reduce((s, p) => s + p.courseCompletionPercent, 0) /
            Object.values(progressById).length) *
            10,
        ) / 10
      : 0;

  const overall = {
    percent: stats?.student?.averageProgressPercent ?? overallPercentFromProgress,
    coursesInProgress: stats?.student?.startedCourses ?? active.length,
    coursesCompleted: stats?.student?.completedCourses ?? 0,
    lessonsCompleted,
    studyHoursLabel: formatStudyHours(studySeconds),
  };

  if (enrollments === null) {
    return (
      <div className="grid gap-4">
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-64 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-10">
      <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div className="max-w-xl">
          <h1 className="font-heading text-3xl font-medium tracking-[-0.02em] sm:text-4xl">
            Bem-vindo de volta,{" "}
            <span className="text-primary italic">{firstName}</span>.
          </h1>
          <p className="mt-3 max-w-md text-sm leading-relaxed text-muted-foreground sm:text-base">
            {active.length > 0
              ? "Veja seu progresso e continue de onde parou."
              : "Quando você tiver uma matrícula ativa, o progresso aparece aqui."}
          </p>
        </div>
        <blockquote className="max-w-xs border-l border-border-gold pl-4 lg:text-right lg:border-l-0 lg:border-r lg:pr-4 lg:pl-0">
          <p className="font-heading text-base text-muted-foreground italic">“{STUDENT_QUOTE}”</p>
        </blockquote>
      </div>

      {active.length === 0 ? (
        <div className="flex flex-col items-start gap-4 rounded-md border border-dashed border-border px-6 py-12">
          <p className="font-heading text-lg font-medium tracking-tight">Nenhum curso em andamento</p>
          <p className="max-w-md text-sm text-muted-foreground">
            Sua área de estudos fica pronta assim que um professor ou administrador liberar o
            acesso a um curso.
          </p>
          <Button asChild variant="outline">
            <Link href="/my-courses">Ir para meus cursos</Link>
          </Button>
        </div>
      ) : (
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1.45fr)_minmax(280px,0.9fr)]">
          <div className="flex flex-col gap-8">
            {current && (
              <CurrentCoursePanel
                title={current.title}
                category={current.category}
                progress={current.progress}
                nextLesson={current.nextLesson}
                href={current.href}
                coverImageUrl={current.coverImageUrl}
              />
            )}

            <section>
              <div className="mb-4 flex items-end justify-between gap-3">
                <div>
                  <p className="text-[0.625rem] font-medium tracking-[0.16em] text-primary uppercase">
                    Meus cursos
                  </p>
                  <h2 className="mt-1 font-heading text-xl font-medium tracking-tight">
                    Em andamento
                  </h2>
                </div>
                <Link
                  href="/my-courses"
                  className="text-sm text-muted-foreground hover:text-primary"
                >
                  Ver todos →
                </Link>
              </div>
              <div className="flex gap-4 overflow-x-auto pb-2">
                {courseCards.map((c) => (
                  <CourseCardCompact
                    key={c.id}
                    title={c.title}
                    subtitle={c.subtitle}
                    progress={c.progress}
                    href={c.href}
                    coverImageUrl={c.coverImageUrl}
                    coverTone={c.coverTone}
                    className="w-[220px] shrink-0"
                  />
                ))}
              </div>
            </section>
          </div>

          <div className="flex flex-col gap-5">
            {nextFromProgress ? (
              <NextLessonPanel
                title={nextFromProgress.title}
                module={nextFromProgress.module}
                duration={nextFromProgress.duration}
                href={nextFromProgress.href}
              />
            ) : (
              <aside className="rounded-md border border-border bg-surface p-5">
                <p className="text-[0.625rem] font-medium tracking-[0.16em] text-primary uppercase">
                  Próxima aula
                </p>
                <p className="mt-3 text-sm text-muted-foreground">
                  Você concluiu as aulas publicadas deste curso.
                </p>
                <Button asChild variant="outline" className="mt-5 w-full">
                  <Link href={current?.href ?? "/my-courses"}>Abrir curso</Link>
                </Button>
              </aside>
            )}

            <aside className="rounded-md border border-border bg-surface p-5">
              <p className="text-[0.625rem] font-medium tracking-[0.16em] text-primary uppercase">
                Seu progresso geral
              </p>
              <div className="mt-5 flex items-center gap-5">
                <ProgressRing value={overall.percent} />
                <ul className="flex flex-1 flex-col gap-2 text-sm">
                  <li className="flex justify-between gap-2">
                    <span className="text-muted-foreground">Em andamento</span>
                    <span className="font-mono text-foreground">{overall.coursesInProgress}</span>
                  </li>
                  <li className="flex justify-between gap-2">
                    <span className="text-muted-foreground">Concluídos</span>
                    <span className="font-mono text-foreground">{overall.coursesCompleted}</span>
                  </li>
                  <li className="flex justify-between gap-2">
                    <span className="text-muted-foreground">Aulas concluídas</span>
                    <span className="font-mono text-foreground">{overall.lessonsCompleted}</span>
                  </li>
                  <li className="flex justify-between gap-2">
                    <span className="text-muted-foreground">Carga assistida</span>
                    <span className="font-mono text-foreground">{overall.studyHoursLabel}</span>
                  </li>
                </ul>
              </div>
            </aside>
          </div>
        </div>
      )}
    </div>
  );
}

function InstructorDashboard({ stats }: { stats: DashboardStats }) {
  const i = stats.instructor!;
  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard label="Seus cursos" value={i.ownedCourses} icon={BookOpen} featured />
        <MetricCard label="Publicados" value={i.publishedCourses} icon={GraduationCap} />
        <MetricCard label="Alunos matriculados" value={i.enrolledStudents} hint="únicos" icon={Users} />
        <MetricCard
          label="Ativos (7 dias)"
          value={i.activeStudentsLast7Days}
          hint="estudaram recentemente"
          icon={TrendingUp}
        />
      </div>
      <LearningChart
        year={stats.year}
        series={stats.activitySeries}
        caption="Ritmo coletivo dos alunos nos seus cursos ao longo do ano."
      />
      <Button asChild variant="outline" className="w-fit gap-1.5">
        <Link href="/courses">
          Ver cursos <ArrowRight className="size-3.5" />
        </Link>
      </Button>
    </div>
  );
}

function AdminDashboard({ stats }: { stats: DashboardStats }) {
  const a = stats.admin!;
  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-3 lg:grid-cols-[1.2fr_1fr_1fr_1fr]">
        <MetricCard label="Alunos" value={a.totalStudents} hint="contas STUDENT" icon={Users} featured />
        <MetricCard label="Cursos publicados" value={a.publishedCourses} icon={GraduationCap} />
        <MetricCard label="Cursos totais" value={a.totalCourses} icon={BookOpen} />
        <MetricCard
          label="Matrículas ativas"
          value={a.activeEnrollments}
          hint={`${a.totalEnrollments} no total`}
          icon={TrendingUp}
        />
      </div>
      <LearningChart
        year={stats.year}
        series={stats.activitySeries}
        caption="Atividade de estudo em toda a plataforma ao longo do ano."
      />
      <div className="flex flex-wrap gap-2">
        <Button asChild variant="outline" className="gap-1.5">
          <Link href="/admin/users">
            Usuários <ArrowRight className="size-3.5" />
          </Link>
        </Button>
        <Button asChild variant="outline" className="gap-1.5">
          <Link href="/courses">
            Cursos <ArrowRight className="size-3.5" />
          </Link>
        </Button>
      </div>
    </div>
  );
}

function DashboardContent() {
  const { user, hasRole } = useAuth();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    apiFetch<DashboardStats>("/dashboard/stats")
      .then((data) => {
        if (active) {
          setStats(data);
          setError(null);
        }
      })
      .catch(() => {
        if (active) {
          setStats(null);
          setError("Não foi possível carregar a visão geral.");
        }
      });
    return () => {
      active = false;
    };
  }, []);

  if (!user) return null;
  const firstName = user.name.split(" ")[0];
  const isStudentHome =
    hasRole("STUDENT") && !hasRole("INSTRUCTOR") && !hasRole("SUPER_ADMIN");

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-8 p-5 sm:p-8">
      {isStudentHome ? (
        <StudentExperience stats={stats} firstName={firstName} />
      ) : (
        <>
          <div>
            <span className="kicker">Visão geral</span>
            <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight sm:text-3xl">
              Olá, <span className="text-primary">{firstName}</span>
            </h1>
            <p className="text-muted-foreground">
              Métricas reais da sua conta — operação, cursos e ritmo de estudo.
            </p>
          </div>

          {error ? (
            <div className="rounded-md border border-dashed border-border px-4 py-8 text-center text-sm text-muted-foreground">
              {error}
            </div>
          ) : !stats ? (
            <div className="grid gap-4">
              <Skeleton className="h-28 w-full" />
              <Skeleton className="h-56 w-full" />
            </div>
          ) : stats.roleView === "ADMIN" && stats.admin ? (
            <AdminDashboard stats={stats} />
          ) : stats.roleView === "INSTRUCTOR" && stats.instructor ? (
            <InstructorDashboard stats={stats} />
          ) : (
            <StudentExperience stats={stats} firstName={firstName} />
          )}
        </>
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
