"use client";

import { Suspense, use, useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import {
  ArrowLeft,
  ArrowRight,
  Award,
  CheckCircle2,
  Circle,
  Lock,
  PlayCircle,
} from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { StudentModuleQuiz } from "@/components/courses/student-module-quiz";
import { apiFetch, ApiError } from "@/lib/api-client";
import type {
  Certificate,
  Enrollment,
  LessonProgress,
  LessonProgressItem,
  ProgressSummary,
  StreamUrl,
} from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

type FlatLesson = LessonProgressItem & { moduleId: string; moduleTitle: string };

function isLessonUnlocked(flatLessons: FlatLesson[], index: number): boolean {
  if (index <= 0) return true;
  return flatLessons[index - 1]?.progressStatus === "COMPLETED";
}

function StudentCourseContent({ courseId }: { courseId: string }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const enrollmentIdParam = searchParams.get("enrollmentId");

  const [enrollment, setEnrollment] = useState<Enrollment | null>(null);
  const [summary, setSummary] = useState<ProgressSummary | null>(null);
  const [selectedLessonId, setSelectedLessonId] = useState<string | null>(null);
  const [streamUrl, setStreamUrl] = useState<string | null>(null);
  const [thumbnailUrl, setThumbnailUrl] = useState<string | null>(null);
  const [loadingStream, setLoadingStream] = useState(false);
  const [finishing, setFinishing] = useState(false);
  const [issuing, setIssuing] = useState(false);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const heartbeatTimer = useRef<ReturnType<typeof setInterval> | null>(null);
  const initialLessonPicked = useRef(false);

  const load = useCallback(async () => {
    try {
      const mine = await apiFetch<Enrollment[]>("/enrollments/me");
      const match =
        mine.find(
          (e) =>
            e.courseId === courseId &&
            e.status === "ACTIVE" &&
            (!enrollmentIdParam || e.id === enrollmentIdParam),
        ) ?? mine.find((e) => e.courseId === courseId && e.status === "ACTIVE");
      if (!match) {
        toast.error("Você não tem matrícula ativa neste curso.");
        router.replace("/my-courses");
        return;
      }
      setEnrollment(match);
      const progress = await apiFetch<ProgressSummary>(`/enrollments/${match.id}/progress/summary`);
      setSummary(progress);
      if (!initialLessonPicked.current) {
        const firstIncomplete =
          progress.modules.flatMap((m) => m.lessons).find((l) => l.progressStatus !== "COMPLETED") ??
          progress.modules.flatMap((m) => m.lessons)[0];
        if (firstIncomplete) {
          setSelectedLessonId(firstIncomplete.lessonId);
        }
        initialLessonPicked.current = true;
      }
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Erro ao carregar curso.",
      );
      router.replace("/my-courses");
    }
  }, [courseId, enrollmentIdParam, router]);

  useEffect(() => {
    void load();
  }, [load]);

  const flatLessons: FlatLesson[] = useMemo(() => {
    if (!summary) return [];
    return summary.modules.flatMap((mod) =>
      mod.lessons.map((lesson) => ({
        ...lesson,
        moduleId: mod.moduleId,
        moduleTitle: mod.moduleTitle,
      })),
    );
  }, [summary]);

  const selectedIndex = useMemo(
    () => flatLessons.findIndex((l) => l.lessonId === selectedLessonId),
    [flatLessons, selectedLessonId],
  );

  const selectedLesson = selectedIndex >= 0 ? flatLessons[selectedIndex] : null;
  const previousLesson = selectedIndex > 0 ? flatLessons[selectedIndex - 1] : null;
  const nextLesson =
    selectedIndex >= 0 && selectedIndex < flatLessons.length - 1
      ? flatLessons[selectedIndex + 1]
      : null;

  const refreshSummary = useCallback(async () => {
    if (!enrollment) return;
    const progress = await apiFetch<ProgressSummary>(
      `/enrollments/${enrollment.id}/progress/summary`,
    );
    setSummary(progress);
    return progress;
  }, [enrollment]);

  useEffect(() => {
    if (!enrollment || !selectedLesson) {
      setStreamUrl(null);
      setThumbnailUrl(null);
      return;
    }

    let cancelled = false;
    const lessonId = selectedLesson.lessonId;
    const videoAssetId = selectedLesson.currentVideoAssetId;

    async function openLesson() {
      setLoadingStream(true);
      setStreamUrl(null);
      setThumbnailUrl(null);
      try {
        await apiFetch(`/enrollments/${enrollment!.id}/progress/lessons/${lessonId}/start`, {
          method: "POST",
        });
        if (videoAssetId) {
          const stream = await apiFetch<StreamUrl>(`/videos/${videoAssetId}/stream-url`);
          if (!cancelled) {
            setStreamUrl(stream.url);
            setThumbnailUrl(stream.thumbnailUrl ?? null);
          }
        }
        if (!cancelled) await refreshSummary();
      } catch (error) {
        if (!cancelled) {
          toast.error(
            error instanceof ApiError
              ? (error.body?.detail ?? error.message)
              : "Não foi possível abrir a aula.",
          );
        }
      } finally {
        if (!cancelled) setLoadingStream(false);
      }
    }

    void openLesson();
    return () => {
      cancelled = true;
    };
    // Dependemos apenas dos campos que abrem o stream; refreshSummary atualiza
    // o objeto selectedLesson e não deve reiniciar a mesma aula em loop.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enrollment, selectedLesson?.lessonId, selectedLesson?.currentVideoAssetId, refreshSummary]);

  useEffect(() => {
    if (heartbeatTimer.current) {
      clearInterval(heartbeatTimer.current);
      heartbeatTimer.current = null;
    }
    if (!enrollment || !selectedLesson || !streamUrl) return;

    heartbeatTimer.current = setInterval(() => {
      const el = videoRef.current;
      if (!el || el.paused) return;
      const position = Math.floor(el.currentTime);
      void apiFetch<LessonProgress>(
        `/enrollments/${enrollment.id}/progress/lessons/${selectedLesson.lessonId}/heartbeat`,
        { method: "POST", body: { positionSeconds: position } },
      )
        .then((p) => {
          if (p.status === "COMPLETED") void refreshSummary();
        })
        .catch(() => {
          /* silencioso */
        });
    }, 10000);

    return () => {
      if (heartbeatTimer.current) clearInterval(heartbeatTimer.current);
    };
  }, [enrollment, selectedLesson, streamUrl, refreshSummary]);

  async function markComplete(silent = false) {
    if (!enrollment || !selectedLesson) return;
    try {
      await apiFetch(
        `/enrollments/${enrollment.id}/progress/lessons/${selectedLesson.lessonId}/complete`,
        { method: "POST" },
      );
      if (!silent) toast.success("Aula marcada como concluída.");
      await refreshSummary();
    } catch (error) {
      if (!silent) {
        toast.error(
          error instanceof ApiError
            ? (error.body?.detail ?? error.message)
            : "Falha ao concluir aula.",
        );
      }
    }
  }

  async function goToNextAfterComplete() {
    await markComplete(true);
    if (nextLesson) {
      setSelectedLessonId(nextLesson.lessonId);
      const sameModule = nextLesson.moduleId === selectedLesson?.moduleId;
      toast.success(
        sameModule
          ? "Próxima aula"
          : `Próximo módulo: ${nextLesson.moduleTitle}`,
      );
    } else {
      toast.success("Você concluiu todas as aulas.");
      await refreshSummary();
    }
  }

  async function finishCourse() {
    if (!enrollment) return;
    setFinishing(true);
    try {
      await apiFetch(`/enrollments/${enrollment.id}/complete-course`, { method: "POST" });
      toast.success("Curso concluído. Emita o certificado abaixo.");
      await refreshSummary();
    } catch (error) {
      toast.error(
        error instanceof ApiError
          ? (error.body?.detail ?? error.message)
          : "Não foi possível concluir o curso.",
      );
    } finally {
      setFinishing(false);
    }
  }

  async function issueCertificate() {
    if (!enrollment) return;
    setIssuing(true);
    try {
      const cert = await apiFetch<Certificate>(`/enrollments/${enrollment.id}/certificate/issue`, {
        method: "POST",
      });
      toast.success("Certificado emitido.");
      await refreshSummary();
      router.push(`/my-certificates/${cert.id}`);
    } catch (error) {
      toast.error(
        error instanceof ApiError
          ? (error.body?.detail ?? error.message)
          : "Não foi possível emitir o certificado.",
      );
    } finally {
      setIssuing(false);
    }
  }

  if (!enrollment || !summary) {
    return (
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-4 p-6 sm:p-8">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  const currentModule = summary.modules.find((m) =>
    m.lessons.some((l) => l.lessonId === selectedLessonId),
  );

  return (
    <div className="flex min-h-[calc(100vh-3.5rem)] flex-1 flex-col">
      <div className="border-b border-border px-4 py-4 sm:px-6">
        <div className="mx-auto flex w-full max-w-6xl flex-col gap-3">
          <Button asChild variant="ghost" size="sm" className="-ml-2 w-fit text-muted-foreground">
            <Link href="/my-courses">
              <ArrowLeft className="size-4" />
              Meus cursos
            </Link>
          </Button>
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-[0.625rem] font-medium tracking-[0.16em] text-primary uppercase">
                Curso
              </p>
              <h1 className="mt-1 font-heading text-2xl font-medium tracking-tight sm:text-3xl">
                {summary.courseTitle}
              </h1>
              <p className="mt-1 text-sm text-muted-foreground">
                {summary.completedLessons} de {summary.totalPublishedLessons} aulas ·{" "}
                <span className="font-mono text-primary">{summary.courseCompletionPercent}%</span>
              </p>
            </div>
            <Progress value={summary.courseCompletionPercent} className="h-px max-w-xs sm:w-64" />
          </div>
        </div>
      </div>

      <div className="mx-auto grid w-full max-w-6xl flex-1 gap-0 lg:grid-cols-[240px_minmax(0,1fr)]">
        <aside className="border-b border-border lg:border-r lg:border-b-0">
          <div className="sticky top-14 max-h-[calc(100vh-3.5rem)] overflow-y-auto p-4 sm:p-5">
            <p className="mb-3 text-[0.625rem] font-medium tracking-[0.14em] text-subtle-foreground uppercase">
              Módulos
            </p>
            <div className="flex flex-col gap-4">
              {summary.modules.map((mod) => (
                <div key={mod.moduleId}>
                  <div className="mb-2 flex items-baseline justify-between gap-2">
                    <p className="text-xs font-medium text-foreground">{mod.moduleTitle}</p>
                    <span className="font-mono text-[10px] text-muted-foreground">
                      {mod.completedLessons}/{mod.totalPublishedLessons}
                    </span>
                  </div>
                  <ul className="flex flex-col border-l border-border">
                    {mod.lessons.map((lesson) => {
                      const flatIndex = flatLessons.findIndex((l) => l.lessonId === lesson.lessonId);
                      const unlocked = isLessonUnlocked(flatLessons, flatIndex);
                      const active = lesson.lessonId === selectedLessonId;
                      const done = lesson.progressStatus === "COMPLETED";
                      return (
                        <li key={lesson.lessonId}>
                          <button
                            type="button"
                            disabled={!unlocked}
                            onClick={() => {
                              if (!unlocked) {
                                toast.error("Conclua a aula anterior antes de avançar.");
                                return;
                              }
                              setSelectedLessonId(lesson.lessonId);
                            }}
                            aria-current={active ? "true" : undefined}
                            className={cn(
                              "relative flex w-full items-start gap-2 py-2 pr-2 pl-3 text-left text-sm transition-colors",
                              !unlocked && "cursor-not-allowed opacity-50",
                              unlocked &&
                                (active
                                  ? "bg-primary-soft text-primary-soft-foreground"
                                  : "text-muted-foreground hover:bg-surface-hover hover:text-foreground"),
                              !unlocked && "text-muted-foreground",
                            )}
                          >
                            {active && unlocked && (
                              <span
                                aria-hidden
                                className="absolute top-1.5 bottom-1.5 left-0 w-0.5 bg-primary"
                              />
                            )}
                            {!unlocked ? (
                              <Lock className="mt-0.5 size-3.5 shrink-0" />
                            ) : done ? (
                              <CheckCircle2 className="mt-0.5 size-3.5 shrink-0 text-primary" />
                            ) : active ? (
                              <PlayCircle className="mt-0.5 size-3.5 shrink-0 text-primary" />
                            ) : (
                              <Circle className="mt-0.5 size-3.5 shrink-0" />
                            )}
                            <span className="leading-snug">{lesson.title}</span>
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                </div>
              ))}
            </div>
          </div>
        </aside>

        <div className="flex flex-col gap-6 p-4 sm:p-6 lg:p-8">
          {selectedLesson ? (
            <>
              <div>
                {currentModule && (
                  <p className="text-[0.625rem] font-medium tracking-[0.14em] text-primary uppercase">
                    {currentModule.moduleTitle}
                  </p>
                )}
                <h2 className="mt-2 font-heading text-xl font-medium tracking-tight sm:text-2xl">
                  {selectedLesson.title}
                </h2>
                <p className="mt-1 text-xs text-muted-foreground">
                  Aula {selectedIndex + 1} de {flatLessons.length}
                  {" · "}
                  {selectedLesson.progressStatus === "COMPLETED"
                    ? "Concluída"
                    : selectedLesson.progressStatus === "IN_PROGRESS"
                      ? "Em andamento"
                      : "Não iniciada"}
                </p>
              </div>

              {loadingStream ? (
                <Skeleton className="aspect-video w-full rounded-md" />
              ) : streamUrl ? (
                <video
                  ref={videoRef}
                  key={streamUrl}
                  src={streamUrl}
                  poster={thumbnailUrl ?? undefined}
                  controls
                  className="aspect-video w-full rounded-md border border-border bg-navy-950"
                  onEnded={() => void goToNextAfterComplete()}
                />
              ) : (
                <div className="flex aspect-video items-center justify-center rounded-md border border-dashed border-border bg-surface text-sm text-muted-foreground">
                  {selectedLesson.currentVideoAssetId
                    ? "Não foi possível carregar o vídeo. Tente selecionar a aula de novo."
                    : "Esta aula ainda não tem vídeo. Você pode marcá-la como concluída e seguir."}
                </div>
              )}

              <div className="flex flex-wrap items-center gap-2">
                <Button
                  variant="outline"
                  disabled={!previousLesson}
                  onClick={() => previousLesson && setSelectedLessonId(previousLesson.lessonId)}
                >
                  <ArrowLeft className="size-4" />
                  Aula anterior
                </Button>
                {selectedLesson.progressStatus !== "COMPLETED" && (
                  <Button variant="secondary" onClick={() => void markComplete()}>
                    <CheckCircle2 className="size-4" />
                    Marcar como concluída
                  </Button>
                )}
                {nextLesson ? (
                  <Button
                    className="ml-auto"
                    disabled={selectedLesson.progressStatus !== "COMPLETED"}
                    onClick={() => {
                      if (selectedLesson.progressStatus !== "COMPLETED") return;
                      setSelectedLessonId(nextLesson.lessonId);
                    }}
                  >
                    {nextLesson.moduleId !== selectedLesson.moduleId
                      ? "Próximo módulo"
                      : "Próxima aula"}
                    <ArrowRight className="size-4" />
                  </Button>
                ) : null}
              </div>

              {(summary.canFinishCourse ||
                summary.canIssueCertificate ||
                summary.certificateId ||
                summary.courseCompletedAt) && (
                <div className="flex flex-col gap-3 border-t border-border pt-6">
                  {summary.canFinishCourse && (
                    <Button size="lg" disabled={finishing} onClick={() => void finishCourse()}>
                      <CheckCircle2 className="size-4" />
                      {finishing ? "Concluindo…" : "Concluir curso"}
                    </Button>
                  )}
                  {summary.canIssueCertificate && (
                    <Button size="lg" disabled={issuing} onClick={() => void issueCertificate()}>
                      <Award className="size-4" />
                      {issuing ? "Emitindo…" : "Emitir certificado"}
                    </Button>
                  )}
                  {summary.certificateId && !summary.canIssueCertificate && (
                    <Button asChild size="lg" variant="outline">
                      <Link href={`/my-certificates/${summary.certificateId}`}>
                        <Award className="size-4" />
                        Ver meu certificado
                      </Link>
                    </Button>
                  )}
                  {summary.courseCompletedAt &&
                    !summary.canIssueCertificate &&
                    !summary.certificateId && (
                      <p className="rounded-md border border-border bg-surface px-4 py-3 text-sm text-muted-foreground">
                        Curso concluído. Para emitir o certificado, o curso precisa ter carga horária
                        definida e (se houver) exercícios de módulo aprovados.
                      </p>
                    )}
                </div>
              )}

              {currentModule && (
                <div className="border-t border-border pt-6">
                  <StudentModuleQuiz key={currentModule.moduleId} moduleId={currentModule.moduleId} />
                </div>
              )}
            </>
          ) : (
            <p className="text-sm text-muted-foreground">Nenhuma aula publicada neste curso.</p>
          )}
        </div>
      </div>
    </div>
  );
}

export default function StudentCoursePage({ params }: { params: Promise<{ courseId: string }> }) {
  const { courseId } = use(params);
  return (
    <ProtectedRoute>
      <DashboardShell>
        <Suspense
          fallback={
            <div className="mx-auto flex w-full max-w-5xl flex-col gap-4 p-6 sm:p-8">
              <Skeleton className="h-8 w-64" />
              <Skeleton className="h-64 w-full" />
            </div>
          }
        >
          <StudentCourseContent courseId={courseId} />
        </Suspense>
      </DashboardShell>
    </ProtectedRoute>
  );
}
