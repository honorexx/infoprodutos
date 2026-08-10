"use client";

import { Suspense, useEffect, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { apiFetch, ApiError } from "@/lib/api-client";
import type { OrderStatus } from "@/lib/types";
import { formatBrlFromCents } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

function ReturnContent() {
  const params = useSearchParams();
  const orderId = params.get("orderId");
  const statusHint = params.get("status");
  const [order, setOrder] = useState<OrderStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!orderId) {
      setError("Pedido não informado.");
      return;
    }
    let cancelled = false;
    let attempts = 0;

    async function poll() {
      try {
        // Em localhost o webhook do MP não chega — sync consulta o pagamento na API do MP.
        const data =
          attempts === 0 || attempts % 2 === 0
            ? await apiFetch<OrderStatus>(`/checkout/orders/${orderId}/sync`, { method: "POST" })
            : await apiFetch<OrderStatus>(`/checkout/orders/${orderId}`);
        if (cancelled) return;
        setOrder(data);
        if (data.status === "PENDING" && attempts < 16) {
          attempts += 1;
          window.setTimeout(() => void poll(), 2500);
        }
      } catch (e) {
        if (!cancelled) {
          setError(e instanceof ApiError ? (e.body?.detail ?? e.message) : "Não foi possível carregar o pedido.");
        }
      }
    }

    void poll();
    return () => {
      cancelled = true;
    };
  }, [orderId]);

  const approved = order?.status === "APPROVED";
  const pending = order?.status === "PENDING" || (!order && statusHint === "pending");
  const failed =
    order?.status === "REJECTED" ||
    order?.status === "CANCELLED" ||
    statusHint === "failure";

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col gap-6 p-6 sm:p-10">
      <div>
        <span className="kicker">Pagamento</span>
        <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight">
          {approved
            ? "Pagamento confirmado"
            : failed
              ? "Pagamento não concluído"
              : pending
                ? "Aguardando confirmação"
                : "Status do pedido"}
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          {approved
            ? "Seus cursos já foram liberados. Você pode começar a estudar agora."
            : pending
              ? "Em localhost o Mercado Pago não redireciona sozinho e o webhook não chega. Esta página consulta o pagamento no MP. Se ficou na tela do MP, volte aqui e abra Meus cursos — a liberação roda ao entrar."
              : failed
                ? "Nenhum acesso foi liberado. Você pode tentar novamente quando quiser."
                : "Consultando o status do seu pedido…"}
        </p>
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      {!order && !error && <Skeleton className="h-24 w-full" />}

      {order && (
        <div className="rounded-md border border-border p-4 text-sm">
          <p>
            Pedido <span className="font-mono text-xs">{order.orderId.slice(0, 8)}…</span>
          </p>
          <p className="mt-1 text-muted-foreground">
            {formatBrlFromCents(order.amountCents)} · {order.kind === "PACKAGE" ? "Pacote" : "Curso"} ·{" "}
            {order.status}
          </p>
          <p className="mt-1 text-muted-foreground">
            {order.courseIds.length} curso{order.courseIds.length === 1 ? "" : "s"} no pedido
          </p>
        </div>
      )}

      <div className="flex flex-wrap gap-2">
        {approved && (
          <Button asChild>
            <Link href="/my-courses">Ir para Meus cursos</Link>
          </Button>
        )}
        <Button asChild variant="outline">
          <Link href="/dashboard">Dashboard</Link>
        </Button>
        {(failed || pending) && (
          <Button asChild variant="ghost">
            <Link href="/cursos">Ver cursos</Link>
          </Button>
        )}
      </div>
    </div>
  );
}

export default function CheckoutReturnPage() {
  return (
    <ProtectedRoute>
      <DashboardShell>
        <Suspense fallback={<Skeleton className="m-8 h-40" />}>
          <ReturnContent />
        </Suspense>
      </DashboardShell>
    </ProtectedRoute>
  );
}
