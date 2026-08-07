"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { motion } from "framer-motion";
import {
  ArrowLeft,
  Check,
  Loader2,
  Quote,
  Sparkles,
  X,
} from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { ProgressBar } from "@/components/ui/progress-bar";
import { Skeleton } from "@/components/ui/skeleton";
import { apiFetch } from "@/lib/api-client";
import { fadeIn, staggerContainer, staggerItem } from "@/lib/animations";
import type { AiJob, AiReview } from "@/lib/types";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

const PIPELINE: { status: string; label: string }[] = [
  { status: "PENDING", label: "Na fila" },
  { status: "TRANSCRIBING", label: "Transcrevendo" },
  { status: "TRANSCRIBED", label: "Transcrito" },
  { status: "GENERATING", label: "Gerando" },
  { status: "AWAITING_REVIEW", label: "Em revisão" },
  { status: "COMPLETED", label: "Concluído" },
];

function pipelineProgress(status: string) {
  if (status === "FAILED" || status === "CANCELLED") return 100;
  const idx = PIPELINE.findIndex((p) => p.status === status);
  if (idx < 0) return 10;
  return Math.round(((idx + 1) / PIPELINE.length) * 100);
}

function AiJobDetailContent() {
  const params = useParams<{ id: string }>();
  const jobId = params.id;
  const [job, setJob] = useState<AiJob | null>(null);
  const [reviews, setReviews] = useState<AiReview[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    const [j, r] = await Promise.all([
      apiFetch<AiJob>(`/ai-jobs/${jobId}`),
      apiFetch<AiReview[]>(`/ai-jobs/${jobId}/reviews`).catch(() => [] as AiReview[]),
    ]);
    setJob(j);
    setReviews(r);
    setSelectedId((prev) => prev ?? r[0]?.questionId ?? null);
  }, [jobId]);

  useEffect(() => {
    void load().catch(() => toast.error("Não foi possível carregar o job."));
    const id = window.setInterval(() => {
      void load().catch(() => undefined);
    }, 3000);
    return () => window.clearInterval(id);
  }, [load]);

  const selected = useMemo(
    () => reviews.find((r) => r.questionId === selectedId) ?? null,
    [reviews, selectedId],
  );

  const pendingCount = reviews.filter((r) => r.reviewStatus === "PENDING").length;

  async function approve(questionId: string) {
    setBusy(true);
    try {
      await apiFetch(`/questions/${questionId}/approve`, { method: "POST" });
      toast.success("Questão aprovada");
      await load();
    } catch {
      toast.error("Falha ao aprovar");
    } finally {
      setBusy(false);
    }
  }

  async function reject(questionId: string) {
    setBusy(true);
    try {
      await apiFetch(`/questions/${questionId}/reject`, {
        method: "POST",
        body: JSON.stringify({ notes: "Rejeitada na revisão" }),
      });
      toast.success("Questão rejeitada");
      await load();
    } catch {
      toast.error("Falha ao rejeitar");
    } finally {
      setBusy(false);
    }
  }

  async function publish(questionId: string) {
    setBusy(true);
    try {
      await apiFetch(`/questions/${questionId}/publish`, { method: "POST" });
      toast.success("Questão publicada");
      await load();
    } catch {
      toast.error("Publique apenas após aprovar");
    } finally {
      setBusy(false);
    }
  }

  async function bulkApprove() {
    const ids = reviews.filter((r) => r.reviewStatus === "PENDING").map((r) => r.questionId);
    if (ids.length === 0) return;
    setBusy(true);
    try {
      await apiFetch("/questions/bulk-approve", {
        method: "POST",
        body: JSON.stringify(ids),
      });
      toast.success(`${ids.length} questões aprovadas`);
      await load();
    } catch {
      toast.error("Falha na aprovação em massa");
    } finally {
      setBusy(false);
    }
  }

  if (!job) {
    return (
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-4 p-6 sm:p-8">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  const running = ["PENDING", "TRANSCRIBING", "TRANSCRIBED", "GENERATING"].includes(job.status);

  return (
    <motion.div
      variants={fadeIn}
      initial="hidden"
      animate="visible"
      className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-6 p-4 sm:p-6 lg:p-8"
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Link
            href="/ai"
            className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="size-3.5" /> Processamentos
          </Link>
          <h1 className="mt-2 font-serif text-2xl font-medium tracking-tight">Revisão de questões</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            A IA propõe. Você decide. Evidências ligadas à transcrição.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge status={job.status} />
          {pendingCount > 0 && (
            <Button variant="accent" size="sm" disabled={busy} onClick={() => void bulkApprove()}>
              Aprovar pendentes ({pendingCount})
            </Button>
          )}
        </div>
      </div>

      <div className="rounded-lg border border-border/70 bg-sidebar p-5 text-sidebar-foreground">
        <div className="mb-3 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-sm">
            <Sparkles className="size-4 text-sidebar-primary" />
            <span className="font-medium">Pipeline</span>
            {running && <Loader2 className="size-3.5 animate-spin text-sidebar-muted" />}
          </div>
          <span className="text-xs text-sidebar-muted">
            {job.provider ?? "—"} · {job.requestedQuestionCount} solicitadas
          </span>
        </div>
        <ProgressBar
          value={pipelineProgress(job.status)}
          trackClassName="bg-sidebar-accent"
          indicatorClassName="bg-sidebar-primary"
          aria-label="Progresso do job de IA"
        />
        <div className="mt-3 flex flex-wrap gap-2">
          {PIPELINE.map((step) => {
            const active = job.status === step.status;
            const done =
              PIPELINE.findIndex((p) => p.status === job.status) >
              PIPELINE.findIndex((p) => p.status === step.status);
            return (
              <span
                key={step.status}
                className={cn(
                  "rounded-md px-2 py-1 text-[11px] font-medium",
                  active && "bg-sidebar-accent text-sidebar-accent-foreground",
                  done && !active && "text-sidebar-primary",
                  !done && !active && "text-sidebar-muted",
                )}
              >
                {step.label}
              </span>
            );
          })}
        </div>
        {job.errorMessage && <p className="mt-3 text-sm text-danger">{job.errorMessage}</p>}
      </div>

      {reviews.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border/70 px-6 py-12 text-center text-sm text-muted-foreground">
          {running
            ? "A IA está trabalhando. As questões aparecem aqui quando o lote estiver pronto para revisão."
            : "Nenhuma questão neste job."}
        </div>
      ) : (
        <div className="grid gap-6 lg:grid-cols-[280px_minmax(0,1fr)]">
          <motion.ul
            variants={staggerContainer}
            initial="hidden"
            animate="visible"
            className="flex flex-col gap-1"
          >
            {reviews.map((review, index) => (
              <motion.li key={review.questionId} variants={staggerItem}>
                <button
                  type="button"
                  onClick={() => setSelectedId(review.questionId)}
                  className={cn(
                    "flex w-full flex-col gap-1 rounded-md border px-3 py-2.5 text-left transition-colors",
                    selectedId === review.questionId
                      ? "border-primary/40 bg-primary-soft"
                      : "border-transparent hover:bg-muted/50",
                  )}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-xs font-semibold text-muted-foreground">
                      Questão {index + 1}
                    </span>
                    <StatusBadge status={review.reviewStatus === "PENDING" ? "AWAITING_REVIEW" : review.questionStatus} />
                  </div>
                  <span className="line-clamp-2 text-sm font-medium">{review.statement}</span>
                </button>
              </motion.li>
            ))}
          </motion.ul>

          {selected && (
            <div className="flex flex-col gap-5 rounded-lg border border-border/70 bg-surface-elevated p-5 sm:p-6">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge status={selected.difficulty} />
                  {selected.topic && (
                    <span className="text-xs text-muted-foreground">{selected.topic}</span>
                  )}
                </div>
                <h2 className="mt-3 font-serif text-xl leading-snug font-medium tracking-tight">
                  {selected.statement}
                </h2>
              </div>

              <ul className="flex flex-col gap-2">
                {selected.options.map((opt) => (
                  <li
                    key={opt.id}
                    className={cn(
                      "rounded-md border px-3 py-2.5 text-sm",
                      opt.correct
                        ? "border-primary/35 bg-primary-soft text-primary-soft-foreground"
                        : "border-border/70",
                    )}
                  >
                    <span className="mr-2 text-xs font-semibold text-muted-foreground">
                      {String.fromCharCode(65 + opt.orderIndex)}.
                    </span>
                    {opt.text}
                    {opt.correct && (
                      <span className="ml-2 text-[11px] font-semibold tracking-wide text-primary uppercase">
                        correta
                      </span>
                    )}
                  </li>
                ))}
              </ul>

              {selected.explanation && (
                <div>
                  <p className="text-xs font-semibold tracking-[0.12em] text-muted-foreground uppercase">
                    Explicação
                  </p>
                  <p className="mt-1.5 text-sm text-muted-foreground">{selected.explanation}</p>
                </div>
              )}

              {typeof selected.evidence?.excerpt === "string" && selected.evidence.excerpt.length > 0 ? (
                <blockquote className="relative rounded-md border border-accent/30 bg-muted/40 px-4 py-3">
                  <Quote className="absolute top-3 right-3 size-4 text-accent/50" />
                  <p className="text-xs font-semibold tracking-[0.12em] text-accent uppercase">
                    Evidência na transcrição
                  </p>
                  <p className="mt-2 font-serif text-sm leading-relaxed italic">
                    “{selected.evidence.excerpt}”
                  </p>
                  <p className="mt-2 text-[11px] text-muted-foreground">
                    {String(selected.evidence.startTimeSeconds ?? "—")}s –{" "}
                    {String(selected.evidence.endTimeSeconds ?? "—")}s
                  </p>
                </blockquote>
              ) : null}

              <div className="flex flex-wrap gap-2 border-t border-border/70 pt-4">
                {selected.reviewStatus === "PENDING" && (
                  <>
                    <Button
                      variant="accent"
                      size="sm"
                      disabled={busy}
                      onClick={() => void approve(selected.questionId)}
                      className="gap-1.5"
                    >
                      <Check className="size-3.5" /> Aprovar
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      disabled={busy}
                      onClick={() => void reject(selected.questionId)}
                      className="gap-1.5"
                    >
                      <X className="size-3.5" /> Rejeitar
                    </Button>
                  </>
                )}
                {selected.questionStatus === "APPROVED" && (
                  <Button
                    variant="default"
                    size="sm"
                    disabled={busy}
                    onClick={() => void publish(selected.questionId)}
                  >
                    Publicar questão
                  </Button>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </motion.div>
  );
}

export default function AiJobDetailPage() {
  return (
    <ProtectedRoute allowedRoles={["SUPER_ADMIN", "INSTRUCTOR"]}>
      <DashboardShell>
        <AiJobDetailContent />
      </DashboardShell>
    </ProtectedRoute>
  );
}
