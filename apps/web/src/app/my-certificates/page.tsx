"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { toast } from "sonner";
import { Award, Download, ExternalLink } from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { apiFetch, ApiError } from "@/lib/api-client";
import { getAccessToken } from "@/lib/token-store";
import { API_BASE_URL } from "@/lib/config";
import type { Certificate } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

function CertificatesContent() {
  const [items, setItems] = useState<Certificate[] | null>(null);

  const load = useCallback(async () => {
    try {
      const data = await apiFetch<Certificate[]>("/certificates/me");
      setItems(data);
    } catch (error) {
      setItems([]);
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Erro ao carregar certificados.",
      );
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  async function downloadPdf(id: string, title: string) {
    try {
      const token = getAccessToken();
      const response = await fetch(`${API_BASE_URL}/certificates/${id}/pdf?t=${Date.now()}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        cache: "no-store",
      });
      if (!response.ok) {
        throw new Error("Falha no download");
      }
      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `certificado-${title.replace(/\s+/g, "-").toLowerCase()}-${Date.now()}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error("Não foi possível baixar o PDF.");
    }
  }

  if (!items) {
    return (
      <div className="mx-auto flex w-full max-w-3xl flex-col gap-4 p-6 sm:p-8">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-28 w-full" />
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 p-6 sm:p-8">
      <div>
        <h1 className="font-serif text-2xl font-medium tracking-tight">Meus certificados</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Certificados emitidos após a conclusão formal do curso.
        </p>
      </div>

      {items.length === 0 ? (
        <div className="rounded-lg border border-dashed border-border px-4 py-10 text-center text-sm text-muted-foreground">
          Você ainda não tem certificados. Conclua um curso em{" "}
          <Link href="/my-courses" className="text-accent underline-offset-4 hover:underline">
            Meus cursos
          </Link>{" "}
          e emita o certificado.
        </div>
      ) : (
        <ul className="flex flex-col gap-3">
          {items.map((cert) => (
            <li
              key={cert.id}
              className="flex flex-col gap-3 rounded-lg border border-border bg-surface px-4 py-4 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <Award className="size-4 shrink-0 text-primary" />
                  <p className="truncate font-medium">{cert.courseTitle}</p>
                  <Badge variant="outline" className="text-[10px]">
                    {cert.status === "ISSUED" ? "Emitido" : cert.status}
                  </Badge>
                </div>
                <p className="mt-1 text-xs text-muted-foreground">
                  {cert.workloadHours}h · conclusão {cert.completionDate} · código {cert.validationCode}
                </p>
              </div>
              <div className="flex shrink-0 flex-wrap gap-2">
                <Button asChild size="sm" variant="outline">
                  <Link href={`/my-certificates/${cert.id}`}>
                    <ExternalLink className="size-3.5" />
                    Ver
                  </Link>
                </Button>
                <Button size="sm" onClick={() => void downloadPdf(cert.id, cert.courseTitle)}>
                  <Download className="size-3.5" />
                  Baixar PDF
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default function MyCertificatesPage() {
  return (
    <ProtectedRoute>
      <DashboardShell>
        <CertificatesContent />
      </DashboardShell>
    </ProtectedRoute>
  );
}
