"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { CheckCircle2, XCircle } from "lucide-react";
import { apiFetch, ApiError } from "@/lib/api-client";
import type { PublicCertificateValidation } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

export default function PublicCertificatePage() {
  const params = useParams<{ codigo: string }>();
  const [data, setData] = useState<PublicCertificateValidation | null | undefined>(undefined);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    try {
      const result = await apiFetch<PublicCertificateValidation>(
        `/public/certificates/validate/${encodeURIComponent(params.codigo)}`,
        { skipAuth: true },
      );
      setData(result);
      setError(null);
    } catch (err) {
      setData(null);
      setError(
        err instanceof ApiError ? (err.body?.detail ?? err.message) : "Certificado não encontrado.",
      );
    }
  }, [params.codigo]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="mx-auto flex w-full max-w-lg flex-col gap-6 px-6 py-16">
        <div>
          <p className="text-xs font-medium uppercase tracking-[0.2em] text-primary">PKS Consultoria</p>
          <h1 className="mt-2 font-serif text-2xl font-medium tracking-tight">Validação de certificado</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Confirme autenticidade sem expor dados privados do aluno.
          </p>
        </div>

        {data === undefined ? (
          <Skeleton className="h-40 w-full" />
        ) : error || !data ? (
          <div className="flex flex-col items-start gap-3 rounded-lg border border-border bg-surface px-4 py-5">
            <div className="flex items-center gap-2 text-sm font-medium text-destructive">
              <XCircle className="size-4" />
              Certificado inválido
            </div>
            <p className="text-sm text-muted-foreground">{error ?? "Código não encontrado."}</p>
          </div>
        ) : (
          <div className="flex flex-col gap-4 rounded-lg border border-border bg-surface px-4 py-5">
            <div className="flex items-center gap-2 text-sm font-medium text-emerald-700 dark:text-emerald-400">
              <CheckCircle2 className="size-4" />
              Certificado válido
              <Badge variant="outline" className="ml-auto text-[10px]">
                {data.status}
              </Badge>
            </div>
            <dl className="grid gap-3 text-sm">
              <div>
                <dt className="text-xs text-muted-foreground">Curso</dt>
                <dd className="font-medium">{data.courseTitle}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Aluno</dt>
                <dd className="font-medium">{data.studentName}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Carga horária</dt>
                <dd className="font-medium">{data.workloadHours} horas</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Conclusão</dt>
                <dd className="font-medium">{data.completionDate}</dd>
              </div>
              <div>
                <dt className="text-xs text-muted-foreground">Código</dt>
                <dd className="font-mono text-xs">{data.validationCode}</dd>
              </div>
            </dl>
          </div>
        )}
      </div>
    </div>
  );
}
