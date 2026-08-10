"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { motion } from "framer-motion";
import { ArrowRight, Cpu, RefreshCw } from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { StatusBadge } from "@/components/status-badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/ui/empty-state";
import { apiFetch } from "@/lib/api-client";
import { fadeIn, staggerContainer, staggerItem } from "@/lib/animations";
import type { AiJob } from "@/lib/types";
import { cn } from "@/lib/utils";

function formatWhen(value: string | null) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function AiJobsContent() {
  const [jobs, setJobs] = useState<AiJob[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const data = await apiFetch<AiJob[]>("/ai-jobs");
      setJobs(data);
      setError(null);
    } catch {
      setJobs([]);
      setError("Não foi possível carregar os processamentos.");
    }
  }, []);

  useEffect(() => {
    void load();
    const id = window.setInterval(() => void load(), 4000);
    return () => window.clearInterval(id);
  }, [load]);

  return (
    <motion.div
      variants={fadeIn}
      initial="hidden"
      animate="visible"
      className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-5 p-4 sm:p-6 lg:p-8"
    >
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <span className="kicker">Inteligência artificial</span>
          <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight sm:text-3xl">
            Processamentos
          </h1>
          <p className="mt-1 max-w-xl text-sm text-muted-foreground">
            Transcrição e geração de questões em rascunho. Nada é publicado sem revisão humana.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => void load()} className="gap-1.5">
          <RefreshCw className="size-3.5" /> Atualizar
        </Button>
      </div>

      {jobs === null ? (
        <div className="overflow-hidden rounded-md border border-border">
          <Skeleton className="h-12 w-full rounded-none" />
          <Skeleton className="h-12 w-full rounded-none" />
          <Skeleton className="h-12 w-full rounded-none" />
        </div>
      ) : jobs.length === 0 ? (
        <EmptyState
          icon={Cpu}
          title={error ?? "Nenhum processamento ainda"}
          description="Abra um curso, associe um vídeo a uma aula e solicite a geração de exercícios."
          action={
            <Link href="/courses">
              <Button size="sm">Ir para cursos</Button>
            </Link>
          }
        />
      ) : (
        <motion.ul
          variants={staggerContainer}
          initial="hidden"
          animate="visible"
          className="flex flex-col divide-y divide-border overflow-hidden rounded-md border border-border bg-surface"
        >
          <li className="grid grid-cols-[1fr_auto] gap-4 bg-background-secondary px-4 py-2 text-[0.625rem] font-medium tracking-[0.12em] text-subtle-foreground uppercase sm:grid-cols-[140px_1fr_120px_auto]">
            <span>Status</span>
            <span className="hidden sm:inline">Job</span>
            <span className="hidden sm:inline">Criado</span>
            <span className="sr-only">Abrir</span>
          </li>
          {jobs.map((job) => {
            const running = ["PENDING", "TRANSCRIBING", "TRANSCRIBED", "GENERATING"].includes(
              job.status,
            );
            return (
              <motion.li key={job.id} variants={staggerItem}>
                <Link
                  href={`/ai/${job.id}`}
                  className="grid grid-cols-[1fr_auto] items-center gap-4 px-4 py-2.5 transition-colors hover:bg-surface-hover sm:grid-cols-[140px_1fr_120px_auto]"
                >
                  <div className="flex items-center gap-2">
                    <span
                      className={cn(
                        "size-1.5 shrink-0 rounded-full",
                        running ? "bg-primary animate-pulse" : "bg-muted-foreground/40",
                        job.status === "COMPLETED" && "bg-primary",
                        job.status === "FAILED" && "bg-danger animate-none",
                        job.status === "AWAITING_REVIEW" && "bg-warning",
                      )}
                    />
                    <StatusBadge status={job.status} />
                  </div>
                  <div className="min-w-0 sm:col-auto">
                    <p className="truncate text-sm font-medium">
                      Aula {job.lessonId.slice(0, 8)}…
                      {job.provider ? ` · ${job.provider}` : ""}
                    </p>
                    <p className="mt-0.5 font-mono text-[11px] text-muted-foreground">
                      {job.requestedQuestionCount} questões · {job.language}
                    </p>
                    {job.errorMessage && (
                      <p className="mt-1 truncate text-xs text-danger">{job.errorMessage}</p>
                    )}
                  </div>
                  <p className="hidden font-mono text-xs text-muted-foreground sm:block">
                    {formatWhen(job.createdAt)}
                  </p>
                  <ArrowRight className="size-3.5 shrink-0 text-muted-foreground" />
                </Link>
              </motion.li>
            );
          })}
        </motion.ul>
      )}
    </motion.div>
  );
}

export default function AiJobsPage() {
  return (
    <ProtectedRoute allowedRoles={["SUPER_ADMIN", "INSTRUCTOR"]}>
      <DashboardShell>
        <AiJobsContent />
      </DashboardShell>
    </ProtectedRoute>
  );
}
