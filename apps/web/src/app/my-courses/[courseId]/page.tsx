"use client";

import { Suspense, use, useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { ArrowLeft, CheckCircle2, Circle, PlayCircle } from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { StudentModuleQuiz } from "@/components/courses/student-module-quiz";
import { apiFetch, ApiError } from "@/lib/api-client";
import type {
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

function StudentCourseContent({ courseId }: { courseId: string }) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const enrollmentIdParam = searchParams.get("enrollmentId");

  const [enrollment, setEnrollment] = useState<Enrollment | null>(null);
  const [summary, setSummary] = useState<ProgressSummary | null>(null);
  const [selectedLessonId, setSelectedLessonId] = useState<string | null>(null);
  const [streamUrl, setStreamUrl] = useState<string | null>(null);
  const [loadingStream, setLoadingStream] = useState(false);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const heartbeatTimer = useRef<ReturnType<typeof setInterval> | null>(null);

  const load = useCallback(async () => {
    try {
      const mine = await apiFetch<Enrollment[]>("/enrollments/me");
      const match =
        mine.find((e) => e.courseId === courseId && e.status === "ACTIVE" && (!enrollmentIdParam || e.id === enrollmentIdParam))
        ?? mine.find((e) => e.courseId === courseId && e.status === "ACTIVE");
      if (!match) {
        toast.error("Você não tem matrícula ativa neste curso.");
        router.replace("/my-courses");
        return;
      }
      setEnrollment(match);
      const progress = await apiFetch<ProgressSummary>(`/enrollments/${match.id}/progress/summary`);
      setSummary(progress);
      const firstIncomplete =
        progress.modules.flatMap((m) => m.lessons).find((l) => l.progressStatus !== "COMPLETED")
        ?? progress.modules.flatMap((m) => m.lessons)[0];
      if (firstIncomplete) {
        setSelectedLessonId(firstIncomplete.lessonId);
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

  const selectedLesson: LessonProgressItem | null = useMemo(() => {
    if (!summary || !selectedLessonId) return null;
    for (const mod of summary.modules) {
      const found = mod.lessons.find((l) => l.lessonId === selectedLessonId);
      if (found) return found;
    }
    return null;
  }, [summary, selectedLessonId]);

  const refreshSummary = useCallback(async () => {
    if (!enrollment) return;
    const progress = await apiFetch<ProgressSummary>(
      `/enrollments/${enrollment.id}/progress/summary`,
    );
    setSummary(progress);
  }, [enrollment]);

  useEffect(() => {
    if (!enrollment || !selectedLesson) {
      setStreamUrl(null);
      return;
    }

    let cancelled = false;

    async function openLesson() {
      if (!enrollment || !selectedLesson) return;
      setLoadingStream(true);
      setStreamUrl(null);
      try {
        await apiFetch(`/enrollments/${enrollment.id}/progress/lessons/${selectedLesson.lessonId}/start`, {
          method: "POST",
        });
        if (selectedLesson.currentVideoAssetId) {
          const stream = await apiFetch<StreamUrl>(
            `/videos/${selectedLesson.currentVideoAssetId}/stream-url`,
          );
          if (!cancelled) setStreamUrl(stream.url);
        }
        await refreshSummary();
      } catch (error) {
        toast.error(
          error instanceof ApiError
            ? (error.body?.detail ?? error.message)
            : "Não foi possível abrir a aula.",
        );
      } finally {
        if (!cancelled) setLoadingStream(false);
      }
    }

    void openLesson();
    return () => {
      cancelled = true;
    };
  }, [enrollment, selectedLesson?.lessonId, selectedLesson?.currentVideoAssetId, refreshSummary]);

  // Heartbeat a cada 10s enquanto o vídeo toca
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
          /* silencioso — próximo tick tenta de novo */
        });
    }, 10000);

    return () => {
      if (heartbeatTimer.current) clearInterval(heartbeatTimer.current);
    };
  }, [enrollment, selectedLesson, streamUrl, refreshSummary]);

  async function markComplete() {
    if (!enrollment || !selectedLesson) return;
    try {
      await apiFetch(
        `/enrollments/${enrollment.id}/progress/lessons/${selectedLesson.lessonId}/complete`,
        { method: "POST" },
      );
      toast.success("Aula marcada como concluída.");
      await refreshSummary();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao concluir aula.",
      );
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
                <h2 className="font-serif text-lg font-medium tracking-tight">{selectedLesson.title}</h2>
                <p className="text-xs text-muted-foreground">
                  Status:{" "}
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
                  onEnded={() => void markComplete()}
                />
              ) : (
                <div className="flex aspect-video items-center justify-center rounded-lg border border-dashed border-border/70 bg-surface text-sm text-muted-foreground">
                  {selectedLesson.currentVideoAssetId
                    ? "Não foi possível carregar o vídeo."
                    : "Esta aula ainda não tem vídeo. Você pode marcá-la como concluída manualmente."}
                </div>
              )}

              {selectedLesson.progressStatus !== "COMPLETED" && (
                <Button onClick={() => void markComplete()} variant="outline" className="w-fit">
                  <CheckCircle2 className="size-4" />
                  Marcar como concluída
                </Button>
              )}

              {summary.modules
                .filter((m) => m.lessons.some((l) => l.lessonId === selectedLesson.lessonId))
                .map((m) => (
                  <StudentModuleQuiz key={m.moduleId} moduleId={m.moduleId} />
                ))}
            </>
          ) : (
            <p className="text-sm text-muted-foreground">Nenhuma aula publicada neste curso.</p>
          )}
        </div>

        <aside className="flex flex-col gap-4">
          {summary.modules.map((mod) => (
            <div key={mod.moduleId} className="rounded-lg border border-border/70 bg-surface-elevated p-3">
              <p className="mb-2 text-xs font-semibold tracking-wide text-muted-foreground uppercase">
                {mod.moduleTitle}
              </p>
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
                          active ? "bg-primary-soft text-primary-soft-foreground" : "hover:bg-muted/60",
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
