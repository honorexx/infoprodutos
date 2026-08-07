"use client";

import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { ClipboardCheck } from "lucide-react";
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
      // Descobre o quiz via endpoint staff/aluno do módulo
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

  if (loading) return <Skeleton className="h-20 w-full" />;
  if (missing || !take) return null;

  const inProgress = attempt?.status === "IN_PROGRESS";
  const graded = attempt && attempt.status !== "IN_PROGRESS";

  return (
    <div className="rounded-md border border-border/70 bg-surface-elevated p-3">
      <div className="mb-2 flex items-center gap-2">
        <ClipboardCheck className="size-4 text-muted-foreground" />
        <p className="text-sm font-medium">{take.title}</p>
        <Badge variant="outline" className="text-[10px]">
          {take.attemptsUsed}
          {take.maxAttempts != null ? `/${take.maxAttempts}` : ""} tentativas
        </Badge>
      </div>

      {!inProgress && !graded && (
        <Button
          size="sm"
          disabled={pending || !take.canStartNewAttempt}
          onClick={() => void start()}
        >
          {take.canStartNewAttempt ? "Iniciar exercício" : "Limite de tentativas atingido"}
        </Button>
      )}

      {inProgress && take.questions.length > 0 && (
        <div className="flex flex-col gap-4">
          {take.questions.map((q, idx) => (
            <fieldset key={q.id} className="flex flex-col gap-2">
              <legend className="text-sm font-medium">
                {idx + 1}. {q.statement}
              </legend>
              <div className="flex flex-col gap-1.5">
                {q.options.map((opt) => {
                  const selected = selections[q.id] === opt.id;
                  return (
                    <button
                      key={opt.id}
                      type="button"
                      onClick={() => void selectOption(q.id, opt.id)}
                      className={cn(
                        "rounded-md border px-3 py-2 text-left text-sm transition-colors",
                        selected
                          ? "border-primary bg-primary-soft text-primary-soft-foreground"
                          : "border-border/70 hover:bg-muted/50",
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
        <div className="flex flex-col gap-3">
          <p className="text-sm">
            Nota: <strong>{attempt.score}%</strong>
            {attempt.passed != null && (attempt.passed ? " — aprovado" : " — reprovado")}
          </p>
          {attempt.answers.map((a) => (
            <div key={a.questionId} className="rounded-md border border-border/60 px-3 py-2 text-sm">
              <p className="font-medium">{a.statement}</p>
              <p className={a.correct ? "text-primary" : "text-destructive"}>
                Sua resposta: {a.selectedOptionText ?? "—"} {a.correct ? "(correta)" : "(incorreta)"}
              </p>
              {a.explanation && (
                <p className="mt-1 text-xs text-muted-foreground">{a.explanation}</p>
              )}
            </div>
          ))}
          {take.canStartNewAttempt && (
            <Button size="sm" variant="outline" disabled={pending} onClick={() => void start()}>
              Nova tentativa
            </Button>
          )}
        </div>
      )}
    </div>
  );
}
