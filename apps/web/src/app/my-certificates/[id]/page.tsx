"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { toast } from "sonner";
import { ArrowLeft, Download } from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { apiFetch, ApiError } from "@/lib/api-client";
import { getAccessToken } from "@/lib/token-store";
import { API_BASE_URL } from "@/lib/config";
import type { Certificate } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

function CertificateDetailContent() {
  const params = useParams<{ id: string }>();
  const [cert, setCert] = useState<Certificate | null | undefined>(undefined);

  const load = useCallback(async () => {
    try {
      const data = await apiFetch<Certificate>(`/certificates/${params.id}`);
      setCert(data);
    } catch (error) {
      setCert(null);
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Certificado não encontrado.",
      );
    }
  }, [params.id]);

  useEffect(() => {
    void load();
  }, [load]);

  async function downloadPdf() {
    if (!cert) return;
    try {
      const token = getAccessToken();
      const response = await fetch(`${API_BASE_URL}/certificates/${cert.id}/pdf?t=${Date.now()}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        cache: "no-store",
      });
      if (!response.ok) throw new Error("Falha no download");
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `certificado-${cert.courseTitle.replace(/\s+/g, "-").toLowerCase()}-${Date.now()}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error("Não foi possível baixar o PDF.");
    }
  }

  if (cert === undefined) {
    return (
      <div className="mx-auto flex w-full max-w-2xl flex-col gap-4 p-6 sm:p-8">
        <Skeleton className="h-8 w-48" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  if (!cert) {
    return (
      <div className="mx-auto flex w-full max-w-2xl flex-col gap-4 p-6 sm:p-8">
        <p className="text-sm text-muted-foreground">Certificado não encontrado.</p>
        <Button asChild variant="outline" size="sm">
          <Link href="/my-certificates">
            <ArrowLeft className="size-3.5" />
            Voltar
          </Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-6 p-6 sm:p-8">
      <div>
        <Button asChild variant="ghost" size="sm" className="-ml-2 mb-3">
          <Link href="/my-certificates">
            <ArrowLeft className="size-3.5" />
            Meus certificados
          </Link>
        </Button>
        <h1 className="font-serif text-2xl font-medium tracking-tight">{cert.courseTitle}</h1>
        <p className="mt-1 text-sm text-muted-foreground">Certificado de conclusão</p>
      </div>

      <dl className="grid gap-3 rounded-lg border border-border bg-surface px-4 py-4 text-sm sm:grid-cols-2">
        <div>
          <dt className="text-xs text-muted-foreground">Aluno</dt>
          <dd className="font-medium">{cert.studentName}</dd>
        </div>
        <div>
          <dt className="text-xs text-muted-foreground">Status</dt>
          <dd>
            <Badge variant="outline">{cert.status === "ISSUED" ? "Emitido" : cert.status}</Badge>
          </dd>
        </div>
        <div>
          <dt className="text-xs text-muted-foreground">Carga horária</dt>
          <dd className="font-medium">{cert.workloadHours} horas</dd>
        </div>
        <div>
          <dt className="text-xs text-muted-foreground">Data de conclusão</dt>
          <dd className="font-medium">{cert.completionDate}</dd>
        </div>
        <div className="sm:col-span-2">
          <dt className="text-xs text-muted-foreground">Código de validação</dt>
          <dd className="font-mono text-sm">{cert.validationCode}</dd>
        </div>
        <div className="sm:col-span-2">
          <dt className="text-xs text-muted-foreground">Validação pública</dt>
          <dd>
            <Link
              href={`/certificados/${cert.validationCode}`}
              className="text-accent underline-offset-4 hover:underline"
              target="_blank"
            >
              Abrir página de validação
            </Link>
          </dd>
        </div>
      </dl>

      <Button onClick={() => void downloadPdf()} className="w-fit">
        <Download className="size-3.5" />
        Baixar PDF
      </Button>
    </div>
  );
}

export default function CertificateDetailPage() {
  return (
    <ProtectedRoute>
      <DashboardShell>
        <CertificateDetailContent />
      </DashboardShell>
    </ProtectedRoute>
  );
}
