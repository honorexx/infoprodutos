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

function StudentCourseContent({ courseId }: { courseId: string }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const enrollmentIdParam = searchParams.get("enrollmentId");

  const [enrollment, setEnrollment] = useState<Enrollment | null>(null);
  const [summary, setSummary] = useState<ProgressSummary | null>(null);
  const [selectedLessonId, setSelectedLessonId] = useState<string | null>(null);
  const [streamUrl, setStreamUrl] = useState<string | null>(null);
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
      return;
    }

    let cancelled = false;
    const lessonId = selectedLesson.lessonId;
    const videoAssetId = selectedLesson.currentVideoAssetId;

    async function openLesson() {
      setLoadingStream(true);
      setStreamUrl(null);
      try {
        await apiFetch(`/enrollments/${enrollment!.id}/progress/lessons/${lessonId}/start`, {
          method: "POST",
        });
        if (videoAssetId) {
          const stream = await apiFetch<StreamUrl>(`/videos/${videoAssetId}/stream-url`);
          if (!cancelled) setStreamUrl(stream.url);
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
    <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-6 p-6 sm:p-8">
      <div className="flex flex-col gap-3">
        <Button asChild variant="ghost" size="sm" className="-ml-2 w-fit">
          <Link href="/my-courses">
            <ArrowLeft className="size-4" />
            Meus cursos
          </Link>
        </Button>
        <div>
          <h1 className="font-serif text-2xl font-medium tracking-tight">{summary.courseTitle}</h1>
          <p className="text-sm text-muted-foreground">
            {summary.completedLessons} de {summary.totalPublishedLessons} aulas concluídas (
            {summary.courseCompletionPercent}%)
          </p>
          <Progress value={summary.courseCompletionPercent} className="mt-2 max-w-md" />
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
        <div className="flex flex-col gap-4">
          {selectedLesson ? (
            <>
              <div>
                {currentModule && (
                  <p className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
                    {currentModule.moduleTitle}
                  </p>
                )}
                <h2 className="font-serif text-lg font-medium tracking-tight">{selectedLesson.title}</h2>
                <p className="text-xs text-muted-foreground">
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
                <Skeleton className="aspect-video w-full" />
              ) : streamUrl ? (
                <video
                  ref={videoRef}
                  key={streamUrl}
                  src={streamUrl}
                  controls
                  className="aspect-video w-full rounded-lg bg-navy-950"
                  onEnded={() => void goToNextAfterComplete()}
                />
              ) : (
                <div className="flex aspect-video items-center justify-center rounded-lg border border-dashed border-border/70 bg-surface text-sm text-muted-foreground">
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
                  <Button variant="outline" onClick={() => void markComplete()}>
                    <CheckCircle2 className="size-4" />
                    Marcar como concluída
                  </Button>
                )}
                {nextLesson ? (
                  <Button
                    className="ml-auto"
                    onClick={() => {
                      if (selectedLesson.progressStatus !== "COMPLETED") {
                        void goToNextAfterComplete();
                      } else {
                        setSelectedLessonId(nextLesson.lessonId);
                      }
                    }}
                  >
                    {nextLesson.moduleId !== selectedLesson.moduleId
                      ? "Próximo módulo"
                      : "Próxima aula"}
                    <ArrowRight className="size-4" />
                  </Button>
                ) : null}
              </div>

              {summary.canFinishCourse && (
                <Button
                  size="lg"
                  className="h-14 w-full text-base"
                  disabled={finishing}
                  onClick={() => void finishCourse()}
                >
                  <CheckCircle2 className="size-5" />
                  {finishing ? "Concluindo…" : "Concluir curso"}
                </Button>
              )}

              {summary.canIssueCertificate && (
                <Button
                  size="lg"
                  className="h-14 w-full text-base"
                  disabled={issuing}
                  onClick={() => void issueCertificate()}
                >
                  <Award className="size-5" />
                  {issuing ? "Emitindo…" : "Emitir certificado"}
                </Button>
              )}

              {summary.certificateId && !summary.canIssueCertificate && (
                <Button asChild size="lg" variant="outline" className="h-14 w-full text-base">
                  <Link href={`/my-certificates/${summary.certificateId}`}>
                    <Award className="size-5" />
                    Ver meu certificado
                  </Link>
                </Button>
              )}

              {summary.courseCompletedAt && !summary.canIssueCertificate && !summary.certificateId && (
                <p className="rounded-lg border border-border/70 bg-surface px-4 py-3 text-sm text-muted-foreground">
                  Curso concluído. Para emitir o certificado, o curso precisa ter carga horária
                  definida e (se houver) exercícios de módulo aprovados. Ajuste no construtor do
                  curso ou conclua os exercícios.
                </p>
              )}

              {currentModule && (
                <StudentModuleQuiz key={currentModule.moduleId} moduleId={currentModule.moduleId} />
              )}
            </>
          ) : (
            <p className="text-sm text-muted-foreground">Nenhuma aula publicada neste curso.</p>
          )}
        </div>

        <aside className="flex flex-col gap-4">
          <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
            Conteúdo do curso
          </p>
          {summary.modules.map((mod) => (
            <div key={mod.moduleId} className="rounded-lg border border-border/70 bg-surface-elevated p-3">
              <div className="mb-2 flex items-baseline justify-between gap-2">
                <p className="text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                  {mod.moduleTitle}
                </p>
                <span className="text-[10px] text-muted-foreground">
                  {mod.completedLessons}/{mod.totalPublishedLessons}
                </span>
              </div>
              <ul className="flex flex-col gap-1">
                {mod.lessons.map((lesson) => {
                  const active = lesson.lessonId === selectedLessonId;
                  const done = lesson.progressStatus === "COMPLETED";
                  return (
                    <li key={lesson.lessonId}>
                      <button
                        type="button"
                        onClick={() => setSelectedLessonId(lesson.lessonId)}
                        className={cn(
                          "flex w-full items-start gap-2 rounded-md px-2 py-2 text-left text-sm transition-colors",
                          active
                            ? "bg-primary-soft text-primary-soft-foreground"
                            : "hover:bg-muted/60",
                        )}
                      >
                        {done ? (
                          <CheckCircle2 className="mt-0.5 size-4 shrink-0 text-primary" />
                        ) : active ? (
                          <PlayCircle className="mt-0.5 size-4 shrink-0" />
                        ) : (
                          <Circle className="mt-0.5 size-4 shrink-0 text-muted-foreground" />
                        )}
                        <span className="leading-snug">{lesson.title}</span>
                      </button>
                    </li>
                  );
                })}
              </ul>
            </div>
          ))}
        </aside>
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
