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
      className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-6 p-6 sm:p-8"
    >
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <span className="kicker">Inteligência artificial</span>
          <h1 className="mt-2 font-serif text-2xl font-medium tracking-tight sm:text-3xl">
            Processamentos de IA
          </h1>
          <p className="mt-1 max-w-xl text-sm text-muted-foreground">
            Transcrição e geração de questões em rascunho. Nada é publicado sem a sua revisão.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => void load()} className="gap-1.5">
          <RefreshCw className="size-3.5" /> Atualizar
        </Button>
      </div>

      {jobs === null ? (
        <div className="flex flex-col gap-2">
          <Skeleton className="h-16 w-full" />
          <Skeleton className="h-16 w-full" />
        </div>
      ) : jobs.length === 0 ? (
        <EmptyState
          icon={Cpu}
          title={error ?? "Nenhum processamento ainda"}
          description="Abra um curso, associe um vídeo a uma aula e solicite a geração de exercícios."
          action={
            <Link href="/courses">
              <Button variant="accent" size="sm">
                Ir para cursos
              </Button>
            </Link>
          }
        />
      ) : (
        <motion.ul
          variants={staggerContainer}
          initial="hidden"
          animate="visible"
          className="flex flex-col divide-y divide-border/70 rounded-lg border border-border/70 bg-surface"
        >
          {jobs.map((job) => (
            <motion.li key={job.id} variants={staggerItem}>
              <Link
                href={`/ai/${job.id}`}
                className="flex items-center justify-between gap-4 px-4 py-4 transition-colors hover:bg-muted/40"
              >
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <StatusBadge status={job.status} />
                    <span className="text-xs text-muted-foreground">
                      {job.requestedQuestionCount} questões · {job.language}
                    </span>
                  </div>
                  <p className="mt-1.5 truncate text-sm font-medium">
                    Aula {job.lessonId.slice(0, 8)}…
                    {job.provider ? ` · ${job.provider}` : ""}
                  </p>
                  {job.errorMessage && (
                    <p className="mt-1 text-xs text-danger">{job.errorMessage}</p>
                  )}
                </div>
                <ArrowRight className="size-4 shrink-0 text-muted-foreground" />
              </Link>
            </motion.li>
          ))}
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
