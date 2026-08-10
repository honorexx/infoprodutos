"use client";

import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { UserPlus } from "lucide-react";
import { apiFetch, ApiError } from "@/lib/api-client";
import { useAuth } from "@/lib/auth-context";
import type { Enrollment, PageResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

const STATUS_LABEL: Record<string, string> = {
  ACTIVE: "Ativa",
  SUSPENDED: "Suspensa",
  CANCELLED: "Cancelada",
  EXPIRED: "Expirada",
};

export function CourseEnrollmentsPanel({ courseId }: { courseId: string }) {
  const { hasRole } = useAuth();
  const isAdmin = hasRole("SUPER_ADMIN");
  const [enrollments, setEnrollments] = useState<Enrollment[] | null>(null);
  const [email, setEmail] = useState("");
  const [pending, setPending] = useState(false);

  const load = useCallback(async () => {
    try {
      const page = await apiFetch<PageResponse<Enrollment>>(
        `/enrollments?courseId=${courseId}&size=50&sort=updatedAt,desc`,
      );
      setEnrollments(page.content);
    } catch (error) {
      setEnrollments([]);
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Erro ao carregar matrículas.",
      );
    }
  }, [courseId]);

  useEffect(() => {
    void load();
  }, [load]);

  async function grant() {
    if (!email.trim()) {
      toast.error("Informe o e-mail do aluno.");
      return;
    }
    setPending(true);
    try {
      await apiFetch<Enrollment>("/enrollments", {
        method: "POST",
        body: { courseId, studentEmail: email.trim() },
      });
      toast.success("Matrícula concedida.");
      setEmail("");
      await load();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Não foi possível matricular.",
      );
    } finally {
      setPending(false);
    }
  }

  async function action(id: string, path: "suspend" | "cancel" | "reactivate", label: string) {
    setPending(true);
    try {
      await apiFetch<Enrollment>(`/enrollments/${id}/${path}`, { method: "POST" });
      toast.success(label);
      await load();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha na operação.",
      );
    } finally {
      setPending(false);
    }
  }

  async function remove(id: string, studentName: string) {
    const ok = window.confirm(
      `Remover permanentemente ${studentName} deste curso?\n\nProgresso, quizzes e certificado desta matrícula serão apagados. Esta ação não pode ser desfeita.`,
    );
    if (!ok) return;
    setPending(true);
    try {
      await apiFetch(`/enrollments/${id}`, { method: "DELETE" });
      toast.success("Aluno removido do curso.");
      await load();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Não foi possível remover.",
      );
    } finally {
      setPending(false);
    }
  }

  return (
    <section className="flex flex-col gap-4 rounded-lg border border-border/70 bg-surface-elevated p-5">
      <div>
        <h2 className="font-serif text-lg font-medium tracking-tight">Alunos matriculados</h2>
        <p className="text-sm text-muted-foreground">
          Concessão manual de acesso. Instrutor cancela; admin também pode remover de vez.
        </p>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <div className="flex-1 space-y-1.5">
          <Label htmlFor="enroll-email">E-mail do aluno</Label>
          <Input
            id="enroll-email"
            type="email"
            placeholder="aluno@exemplo.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") void grant();
            }}
          />
        </div>
        <Button onClick={() => void grant()} disabled={pending} className="sm:mb-0">
          <UserPlus className="size-4" />
          Matricular
        </Button>
      </div>

      {!enrollments ? (
        <Skeleton className="h-24 w-full" />
      ) : enrollments.length === 0 ? (
        <p className="rounded-md border border-dashed border-border/70 px-4 py-6 text-center text-sm text-muted-foreground">
          Nenhuma matrícula neste curso ainda.
        </p>
      ) : (
        <ul className="divide-y divide-border/60">
          {enrollments.map((item) => (
            <li key={item.id} className="flex flex-col gap-3 py-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="text-sm font-medium">{item.studentName}</p>
                <p className="text-xs text-muted-foreground">{item.studentEmail}</p>
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <Badge variant="outline">{STATUS_LABEL[item.status] ?? item.status}</Badge>
                {item.status === "ACTIVE" && (
                  <>
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={pending}
                      onClick={() => void action(item.id, "suspend", "Matrícula suspensa.")}
                    >
                      Suspender
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      disabled={pending}
                      onClick={() => void action(item.id, "cancel", "Matrícula cancelada.")}
                    >
                      Cancelar
                    </Button>
                  </>
                )}
                {(item.status === "SUSPENDED" || item.status === "CANCELLED") && (
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={pending}
                    onClick={() => void action(item.id, "reactivate", "Matrícula reativada.")}
                  >
                    Reativar
                  </Button>
                )}
                {isAdmin && (
                  <Button
                    size="sm"
                    variant="destructive"
                    disabled={pending}
                    onClick={() => void remove(item.id, item.studentName)}
                  >
                    Remover
                  </Button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
