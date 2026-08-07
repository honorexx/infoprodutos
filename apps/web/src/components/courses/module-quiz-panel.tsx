"use client";

import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { ClipboardList, Plus, Trash2 } from "lucide-react";
import { apiFetch, ApiError } from "@/lib/api-client";
import type { CourseModule, Lesson, QuestionStaff, QuizDetail } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

type OptionDraft = { text: string; correct: boolean; orderIndex: number };

const emptyOptions = (): OptionDraft[] =>
  [0, 1, 2, 3].map((i) => ({ text: "", correct: i === 0, orderIndex: i }));

export function ModuleQuizPanel({
  module,
}: {
  module: CourseModule;
}) {
  const [quiz, setQuiz] = useState<QuizDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [pending, setPending] = useState(false);
  const [lessonId, setLessonId] = useState(module.lessons[0]?.id ?? "");
  const [statement, setStatement] = useState("");
  const [explanation, setExplanation] = useState("");
  const [difficulty, setDifficulty] = useState("MEDIUM");
  const [options, setOptions] = useState<OptionDraft[]>(emptyOptions);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await apiFetch<QuizDetail>(`/modules/${module.id}/quiz`);
      setQuiz(data);
    } catch (error) {
      setQuiz(null);
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Erro ao carregar quiz.",
      );
    } finally {
      setLoading(false);
    }
  }, [module.id]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!lessonId && module.lessons[0]) {
      setLessonId(module.lessons[0].id);
    }
  }, [module.lessons, lessonId]);

  function setCorrectIndex(index: number) {
    setOptions((prev) => prev.map((o, i) => ({ ...o, correct: i === index })));
  }

  async function createQuestion() {
    if (!lessonId) {
      toast.error("Selecione a aula de origem.");
      return;
    }
    if (statement.trim().length < 3) {
      toast.error("Enunciado muito curto.");
      return;
    }
    if (options.some((o) => !o.text.trim())) {
      toast.error("Preencha as 4 alternativas.");
      return;
    }
    if (options.filter((o) => o.correct).length !== 1) {
      toast.error("Marque exatamente 1 alternativa correta.");
      return;
    }
    setPending(true);
    try {
      await apiFetch<QuestionStaff>(`/modules/${module.id}/quiz/questions`, {
        method: "POST",
        body: {
          lessonId,
          statement: statement.trim(),
          explanation: explanation.trim() || null,
          difficulty,
          topic: null,
          options: options.map((o) => ({
            text: o.text.trim(),
            correct: o.correct,
            orderIndex: o.orderIndex,
          })),
        },
      });
      toast.success("Questão criada (rascunho).");
      setDialogOpen(false);
      setStatement("");
      setExplanation("");
      setOptions(emptyOptions());
      await load();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao criar questão.",
      );
    } finally {
      setPending(false);
    }
  }

  async function publishQuestion(id: string) {
    setPending(true);
    try {
      await apiFetch<QuestionStaff>(`/questions/${id}/publish-manual`, { method: "POST" });
      toast.success("Questão publicada.");
      await load();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao publicar.",
      );
    } finally {
      setPending(false);
    }
  }

  async function deleteQuestion(id: string) {
    if (!confirm("Remover esta questão?")) return;
    setPending(true);
    try {
      await apiFetch(`/questions/${id}`, { method: "DELETE" });
      toast.success("Questão removida.");
      await load();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao remover.",
      );
    } finally {
      setPending(false);
    }
  }

  async function publishQuiz() {
    if (!quiz?.id) return;
    setPending(true);
    try {
      await apiFetch<QuizDetail>(`/quizzes/${quiz.id}/publish`, { method: "POST" });
      toast.success("Quiz publicado.");
      await load();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao publicar quiz.",
      );
    } finally {
      setPending(false);
    }
  }

  if (loading) {
    return <Skeleton className="h-24 w-full" />;
  }

  const questions = quiz?.questions ?? [];

  return (
    <div className="rounded-md border border-dashed border-border/70 bg-surface/40 p-3">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <ClipboardList className="size-4 text-muted-foreground" />
          <p className="text-sm font-medium">Exercício do módulo</p>
          {quiz?.status && (
            <Badge variant="outline" className="text-[10px]">
              {quiz.status === "PUBLISHED" ? "Publicado" : "Rascunho"}
            </Badge>
          )}
          <span className="text-xs text-muted-foreground">
            {quiz?.publishedQuestionCount ?? 0} publicada(s)
          </span>
        </div>
        <div className="flex items-center gap-1.5">
          {quiz?.id && quiz.status !== "PUBLISHED" && (quiz.publishedQuestionCount ?? 0) > 0 && (
            <Button size="sm" variant="outline" disabled={pending} onClick={() => void publishQuiz()}>
              Publicar quiz
            </Button>
          )}
          <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
            <DialogTrigger asChild>
              <Button size="sm" disabled={module.lessons.length === 0 || pending}>
                <Plus className="size-3.5" />
                Nova questão
              </Button>
            </DialogTrigger>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
              <DialogHeader>
                <DialogTitle>Questão manual</DialogTitle>
                <DialogDescription>
                  Exatamente 4 alternativas e 1 correta. A questão nasce em rascunho.
                </DialogDescription>
              </DialogHeader>
              <div className="flex flex-col gap-3">
                <div className="flex flex-col gap-1.5">
                  <Label>Aula de origem</Label>
                  <Select value={lessonId} onValueChange={setLessonId}>
                    <SelectTrigger>
                      <SelectValue placeholder="Selecione" />
                    </SelectTrigger>
                    <SelectContent>
                      {module.lessons.map((l: Lesson) => (
                        <SelectItem key={l.id} value={l.id}>
                          {l.title}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor={`stmt-${module.id}`}>Enunciado</Label>
                  <Textarea
                    id={`stmt-${module.id}`}
                    rows={3}
                    value={statement}
                    onChange={(e) => setStatement(e.target.value)}
                  />
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label>Dificuldade</Label>
                  <Select value={difficulty} onValueChange={setDifficulty}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="EASY">Fácil</SelectItem>
                      <SelectItem value="MEDIUM">Média</SelectItem>
                      <SelectItem value="HARD">Difícil</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex flex-col gap-2">
                  <Label>Alternativas</Label>
                  {options.map((opt, index) => (
                    <div key={opt.orderIndex} className="flex items-center gap-2">
                      <input
                        type="radio"
                        name={`correct-${module.id}`}
                        checked={opt.correct}
                        onChange={() => setCorrectIndex(index)}
                        aria-label={`Marcar alternativa ${index + 1} como correta`}
                      />
                      <Input
                        value={opt.text}
                        placeholder={`Alternativa ${index + 1}`}
                        onChange={(e) =>
                          setOptions((prev) =>
                            prev.map((o, i) => (i === index ? { ...o, text: e.target.value } : o)),
                          )
                        }
                      />
                    </div>
                  ))}
                </div>
                <div className="flex flex-col gap-1.5">
                  <Label htmlFor={`exp-${module.id}`}>Explicação (opcional)</Label>
                  <Textarea
                    id={`exp-${module.id}`}
                    rows={2}
                    value={explanation}
                    onChange={(e) => setExplanation(e.target.value)}
                  />
                </div>
              </div>
              <DialogFooter>
                <Button disabled={pending} onClick={() => void createQuestion()}>
                  Criar questão
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      {questions.length === 0 ? (
        <p className="text-xs text-muted-foreground">Nenhuma questão neste módulo ainda.</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {questions.map((q) => (
            <li
              key={q.id}
              className="flex flex-wrap items-start justify-between gap-2 rounded-md border border-border/60 bg-card px-3 py-2"
            >
              <div className="min-w-0 flex-1">
                <p className="text-sm leading-snug">{q.statement}</p>
                <div className="mt-1 flex flex-wrap gap-1.5">
                  <Badge variant="outline" className="text-[10px]">
                    {q.status}
                  </Badge>
                  <Badge variant="outline" className="text-[10px]">
                    {q.difficulty}
                  </Badge>
                  <span className="text-[10px] text-muted-foreground">
                    {q.options.filter((o) => o.correct).length === 1
                      ? `Correta: ${q.options.find((o) => o.correct)?.text ?? "—"}`
                      : "estrutura inválida"}
                  </span>
                </div>
              </div>
              <div className="flex shrink-0 gap-1">
                {q.status !== "PUBLISHED" && (
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={pending}
                    onClick={() => void publishQuestion(q.id)}
                  >
                    Publicar
                  </Button>
                )}
                <Button
                  size="icon-sm"
                  variant="ghost"
                  disabled={pending}
                  onClick={() => void deleteQuestion(q.id)}
                >
                  <Trash2 className="size-3.5" />
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
