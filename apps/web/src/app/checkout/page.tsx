"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "sonner";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { ApiError } from "@/lib/api-client";
import { startCheckout } from "@/lib/checkout";
import { Skeleton } from "@/components/ui/skeleton";

function CheckoutKickoff() {
  const params = useSearchParams();
  const router = useRouter();
  const [message, setMessage] = useState("Preparando pagamento…");

  useEffect(() => {
    const courseId = params.get("courseId") ?? undefined;
    const packageId = params.get("packageId") ?? undefined;
    if (!courseId && !packageId) {
      setMessage("Nenhum curso ou pacote selecionado.");
      return;
    }

    let cancelled = false;
    async function run() {
      try {
        await startCheckout({ courseId, packageId });
      } catch (error) {
        if (cancelled) return;
        const detail =
          error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao iniciar checkout.";
        toast.error(detail);
        setMessage(detail);
        router.replace("/dashboard");
      }
    }
    void run();
    return () => {
      cancelled = true;
    };
  }, [params, router]);

  return (
    <div className="mx-auto flex w-full max-w-lg flex-1 flex-col items-center justify-center gap-3 p-8 text-center">
      <Skeleton className="h-8 w-48" />
      <p className="text-sm text-muted-foreground">{message}</p>
    </div>
  );
}

export default function CheckoutPage() {
  return (
    <ProtectedRoute>
      <DashboardShell>
        <Suspense fallback={<Skeleton className="m-8 h-24" />}>
          <CheckoutKickoff />
        </Suspense>
      </DashboardShell>
    </ProtectedRoute>
  );
}
