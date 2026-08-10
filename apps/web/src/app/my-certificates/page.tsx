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
      const stamp = String(Math.floor(performance.now()));
      const response = await fetch(`${API_BASE_URL}/certificates/${id}/pdf?t=${stamp}`, {
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
      a.download = `certificado-${title.replace(/\s+/g, "-").toLowerCase()}.pdf`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      toast.error("Não foi possível baixar o PDF.");
    }
  }

  if (!items) {
    return (
      <div className="mx-auto flex w-full max-w-4xl flex-col gap-4 p-6 sm:p-8">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-28 w-full" />
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-1 flex-col gap-8 p-6 sm:p-8">
      <div>
        <span className="kicker">Conquistas</span>
        <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight sm:text-3xl">
          Certificados
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Documentos emitidos após a conclusão formal do curso — com código de validação.
        </p>
      </div>

      {items.length === 0 ? (
        <div className="rounded-md border border-dashed border-border px-4 py-10 text-center text-sm text-muted-foreground">
          Você ainda não tem certificados. Conclua um curso em{" "}
          <Link href="/my-courses" className="text-primary underline-offset-4 hover:underline">
            Meus cursos
          </Link>
          .
        </div>
      ) : (
        <ul className="divide-y divide-border border-t border-border">
          {items.map((cert) => (
            <li
              key={cert.id}
              className="flex flex-col gap-4 py-5 sm:flex-row sm:items-center sm:justify-between"
            >
              <div className="flex items-start gap-3">
                <span className="mt-0.5 flex size-9 items-center justify-center rounded-md bg-primary-soft text-primary">
                  <Award className="size-4" />
                </span>
                <div>
                  <h2 className="font-heading text-lg font-medium tracking-tight">
                    {cert.courseTitle}
                  </h2>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Emitido em {new Date(cert.issuedAt).toLocaleDateString("pt-BR")} · Código{" "}
                    <span className="font-mono text-primary-hover">{cert.validationCode}</span>
                  </p>
                  <Badge variant="gold" className="mt-2">
                    {cert.status === "ISSUED" ? "Emitido" : cert.status}
                  </Badge>
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                <Button asChild variant="outline" size="sm">
                  <Link href={`/my-certificates/${cert.id}`}>
                    <ExternalLink className="size-3.5" /> Visualizar
                  </Link>
                </Button>
                <Button
                  size="sm"
                  variant="secondary"
                  onClick={() => void downloadPdf(cert.id, cert.courseTitle)}
                >
                  <Download className="size-3.5" /> Baixar PDF
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
