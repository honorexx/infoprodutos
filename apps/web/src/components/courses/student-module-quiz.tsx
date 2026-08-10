"use client";

import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Check, ClipboardCheck, X } from "lucide-react";
import { apiFetch, ApiError } from "@/lib/api-client";
import type { QuizAttempt, QuizTake } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

export function StudentModuleQuiz({ moduleId }: { moduleId: string }) {
  const [take, setTake] = useState<QuizTake | null>(null);
  const [attempt, setAttempt] = useState<QuizAttempt | null>(null);
  const [selections, setSelections] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState(false);
  const [missing, setMissing] = useState(false);

  const loadTake = useCallback(async () => {
    setLoading(true);
    setMissing(false);
    try {
      const detail = await apiFetch<{ id: string | null; status: string; publishedQuestionCount: number }>(
        `/modules/${moduleId}/quiz`,
      );
      if (!detail.id || detail.status !== "PUBLISHED" || detail.publishedQuestionCount === 0) {
        setMissing(true);
        setTake(null);
        return;
      }
      const view = await apiFetch<QuizTake>(`/quizzes/${detail.id}/take`);
      setTake(view);
      if (view.inProgressAttemptId) {
        const a = await apiFetch<QuizAttempt>(`/quiz-attempts/${view.inProgressAttemptId}`);
        setAttempt(a);
      } else {
        setAttempt((prev) => (prev && prev.status !== "IN_PROGRESS" ? prev : null));
      }
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        setMissing(true);
        setTake(null);
        return;
      }
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Erro ao carregar exercício.",
      );
    } finally {
      setLoading(false);
    }
  }, [moduleId]);

  useEffect(() => {
    void loadTake();
  }, [loadTake]);

  async function start() {
    if (!take) return;
    setPending(true);
    try {
      const a = await apiFetch<QuizAttempt>(`/quizzes/${take.quizId}/attempts`, { method: "POST" });
      setAttempt(a);
      setSelections({});
      await loadTake();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Não foi possível iniciar.",
      );
    } finally {
      setPending(false);
    }
  }

  async function selectOption(questionId: string, optionId: string) {
    if (!attempt || attempt.status !== "IN_PROGRESS") return;
    setSelections((prev) => ({ ...prev, [questionId]: optionId }));
    try {
      await apiFetch<QuizAttempt>(`/quiz-attempts/${attempt.id}/answers`, {
        method: "POST",
        body: { questionId, selectedOptionId: optionId },
      });
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao salvar resposta.",
      );
    }
  }

  async function submit() {
    if (!attempt) return;
    setPending(true);
    try {
      const graded = await apiFetch<QuizAttempt>(`/quiz-attempts/${attempt.id}/submit`, {
        method: "POST",
      });
      setAttempt(graded);
      toast.success(
        graded.passed
          ? `Aprovado — nota ${graded.score}%`
          : `Nota ${graded.score}% — não atingiu a média.`,
      );
      await loadTake();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao enviar.",
      );
    } finally {
      setPending(false);
    }
  }

  if (loading) return <Skeleton className="h-24 w-full" />;
  if (missing || !take) return null;

  const inProgress = attempt?.status === "IN_PROGRESS";
  const graded = attempt && attempt.status !== "IN_PROGRESS";

  return (
    <section className="rounded-md border border-border bg-surface p-5 sm:p-6">
      <div className="mb-5 flex flex-wrap items-center gap-2">
        <ClipboardCheck className="size-4 text-primary" />
        <h3 className="font-heading text-lg font-medium tracking-tight">{take.title}</h3>
        <Badge variant="outline" className="normal-case tracking-normal">
          {take.attemptsUsed}
          {take.maxAttempts != null ? `/${take.maxAttempts}` : ""} tentativas
        </Badge>
      </div>

      {!inProgress && !graded && (
        <Button disabled={pending || !take.canStartNewAttempt} onClick={() => void start()}>
          {take.canStartNewAttempt ? "Iniciar exercício" : "Limite de tentativas atingido"}
        </Button>
      )}

      {inProgress && take.questions.length > 0 && (
        <div className="flex flex-col gap-8">
          {take.questions.map((q, idx) => (
            <fieldset key={q.id} className="flex flex-col gap-3">
              <legend className="text-base font-medium text-foreground">
                <span className="mr-2 font-mono text-sm text-primary">
                  {String(idx + 1).padStart(2, "0")}
                </span>
                {q.statement}
              </legend>
              <div className="flex flex-col gap-2">
                {q.options.map((opt) => {
                  const selected = selections[q.id] === opt.id;
                  return (
                    <button
                      key={opt.id}
                      type="button"
                      onClick={() => void selectOption(q.id, opt.id)}
                      className={cn(
                        "min-h-12 rounded-md border px-4 py-3 text-left text-sm transition-colors",
                        selected
                          ? "border-border-gold-active bg-primary-soft text-primary-soft-foreground"
                          : "border-border text-foreground hover:border-border-gold hover:bg-surface-hover",
                      )}
                    >
                      {opt.text}
                    </button>
                  );
                })}
              </div>
            </fieldset>
          ))}
          <Button disabled={pending} onClick={() => void submit()} className="w-fit">
            Enviar respostas
          </Button>
        </div>
      )}

      {graded && attempt && (
        <div className="flex flex-col gap-4">
          <p className="text-sm text-muted-foreground">
            Nota:{" "}
            <span className="font-mono text-primary">{attempt.score}%</span>
            {attempt.passed != null && (attempt.passed ? " — aprovado" : " — abaixo da média")}
          </p>
          {attempt.answers.map((a) => (
            <div
              key={a.questionId}
              className={cn(
                "rounded-md border px-4 py-3 text-sm",
                a.correct ? "border-border-gold/40 bg-primary-soft/40" : "border-danger/30 bg-danger/5",
              )}
            >
              <p className="flex items-start gap-2 font-medium">
                {a.correct ? (
                  <Check className="mt-0.5 size-4 shrink-0 text-primary" />
                ) : (
                  <X className="mt-0.5 size-4 shrink-0 text-danger" />
                )}
                {a.statement}
              </p>
              <p className="mt-2 pl-6 text-muted-foreground">
                Sua resposta: {a.selectedOptionText ?? "—"}
                <span className={a.correct ? " text-primary" : " text-danger"}>
                  {a.correct ? " (correta)" : " (incorreta)"}
                </span>
              </p>
              {a.explanation && (
                <p className="mt-2 pl-6 text-xs text-subtle-foreground">{a.explanation}</p>
              )}
            </div>
          ))}
          {take.canStartNewAttempt && (
            <Button variant="outline" disabled={pending} onClick={() => void start()}>
              Nova tentativa
            </Button>
          )}
        </div>
      )}
    </section>
  );
}
