"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { motion } from "framer-motion";
import { ArrowLeft, Check, Loader2, Quote, X } from "lucide-react";
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

function reviewBadgeStatus(review: AiReview) {
  if (review.reviewStatus === "PENDING") return "AWAITING_REVIEW";
  if (review.reviewStatus === "REJECTED") return "FAILED";
  return review.questionStatus;
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
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-4 p-4 sm:p-6 lg:p-8">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  const running = ["PENDING", "TRANSCRIBING", "TRANSCRIBED", "GENERATING"].includes(job.status);
  const currentIdx = PIPELINE.findIndex((p) => p.status === job.status);

  return (
    <motion.div
      variants={fadeIn}
      initial="hidden"
      animate="visible"
      className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-5 p-4 sm:p-6 lg:p-8"
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <Link
            href="/ai"
            className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground hover:text-foreground"
          >
            <ArrowLeft className="size-3.5" /> Processamentos
          </Link>
          <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight">
            Revisão de questões
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            A IA propõe. Você decide. Evidências ligadas à transcrição.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge status={job.status} />
          {pendingCount > 0 && (
            <Button size="sm" disabled={busy} onClick={() => void bulkApprove()}>
              Aprovar pendentes ({pendingCount})
            </Button>
          )}
        </div>
      </div>

      <section className="rounded-md border border-border bg-navy-950 p-4 text-sidebar-foreground sm:p-5">
        <div className="mb-3 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-sm">
            <span className="font-medium text-primary-soft-foreground">Pipeline</span>
            {running && <Loader2 className="size-3.5 animate-spin text-primary" />}
          </div>
          <span className="font-mono text-[11px] text-sidebar-muted">
            {job.provider ?? "—"} · {job.requestedQuestionCount} solicitadas
          </span>
        </div>
        <ProgressBar
          value={pipelineProgress(job.status)}
          trackClassName="bg-navy-800"
          indicatorClassName="bg-primary"
          aria-label="Progresso do job de IA"
        />
        <ol className="mt-4 flex flex-wrap gap-1.5">
          {PIPELINE.map((step, idx) => {
            const active = job.status === step.status;
            const done = currentIdx > idx;
            return (
              <li
                key={step.status}
                className={cn(
                  "rounded-sm border px-2 py-1 text-[11px] font-medium",
                  active && "border-border-gold-active bg-primary-soft text-primary-soft-foreground",
                  done && !active && "border-transparent text-primary",
                  !done && !active && "border-transparent text-sidebar-muted",
                )}
              >
                <span className="mr-1.5 font-mono opacity-60">
                  {String(idx + 1).padStart(2, "0")}
                </span>
                {step.label}
              </li>
            );
          })}
        </ol>
        {job.errorMessage && <p className="mt-3 text-sm text-danger">{job.errorMessage}</p>}
      </section>

      {reviews.length === 0 ? (
        <div className="rounded-md border border-dashed border-border px-6 py-12 text-center text-sm text-muted-foreground">
          {running
            ? "Processando. As questões aparecem aqui quando o lote estiver pronto para revisão."
            : "Nenhuma questão neste job."}
        </div>
      ) : (
        <div className="grid gap-4 lg:grid-cols-[260px_minmax(0,1fr)]">
          <motion.ul
            variants={staggerContainer}
            initial="hidden"
            animate="visible"
            className="flex max-h-[70vh] flex-col gap-0.5 overflow-y-auto rounded-md border border-border p-1.5"
          >
            {reviews.map((review, index) => (
              <motion.li key={review.questionId} variants={staggerItem}>
                <button
                  type="button"
                  onClick={() => setSelectedId(review.questionId)}
                  className={cn(
                    "flex w-full flex-col gap-1 rounded-sm border px-2.5 py-2 text-left transition-colors",
                    selectedId === review.questionId
                      ? "border-border-gold-active bg-primary-soft"
                      : "border-transparent hover:bg-surface-hover",
                  )}
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="font-mono text-[10px] text-primary">
                      {String(index + 1).padStart(2, "0")}
                    </span>
                    <StatusBadge status={reviewBadgeStatus(review)} className="scale-90 origin-right" />
                  </div>
                  <span className="line-clamp-2 text-xs font-medium leading-snug">
                    {review.statement}
                  </span>
                </button>
              </motion.li>
            ))}
          </motion.ul>

          {selected && (
            <div className="flex flex-col gap-5 rounded-md border border-border bg-surface p-4 sm:p-6">
              <div>
                <div className="flex flex-wrap items-center gap-2">
                  <StatusBadge status={selected.difficulty} />
                  {selected.topic && (
                    <span className="text-xs text-muted-foreground">{selected.topic}</span>
                  )}
                </div>
                <h2 className="mt-3 font-heading text-xl leading-snug font-medium tracking-tight">
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
                        ? "border-border-gold-active bg-primary-soft text-primary-soft-foreground"
                        : "border-border",
                    )}
                  >
                    <span className="mr-2 font-mono text-xs text-muted-foreground">
                      {String.fromCharCode(65 + opt.orderIndex)}.
                    </span>
                    {opt.text}
                    {opt.correct && (
                      <span className="ml-2 text-[11px] font-medium tracking-wide text-primary uppercase">
                        correta
                      </span>
                    )}
                  </li>
                ))}
              </ul>

              {selected.explanation && (
                <div>
                  <p className="text-[0.625rem] font-medium tracking-[0.12em] text-subtle-foreground uppercase">
                    Explicação
                  </p>
                  <p className="mt-1.5 text-sm text-muted-foreground">{selected.explanation}</p>
                </div>
              )}

              {typeof selected.evidence?.excerpt === "string" &&
              selected.evidence.excerpt.length > 0 ? (
                <blockquote className="relative rounded-md border border-border-gold bg-background-secondary px-4 py-3">
                  <Quote className="absolute top-3 right-3 size-4 text-primary/40" />
                  <p className="text-[0.625rem] font-medium tracking-[0.12em] text-primary uppercase">
                    Evidência na transcrição
                  </p>
                  <p className="mt-2 font-heading text-sm leading-relaxed italic">
                    “{selected.evidence.excerpt}”
                  </p>
                  <p className="mt-2 font-mono text-[11px] text-muted-foreground">
                    {String(selected.evidence.startTimeSeconds ?? "—")}s –{" "}
                    {String(selected.evidence.endTimeSeconds ?? "—")}s
                  </p>
                </blockquote>
              ) : null}

              <div className="flex flex-wrap gap-2 border-t border-border pt-4">
                {selected.reviewStatus === "PENDING" && (
                  <>
                    <Button
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
