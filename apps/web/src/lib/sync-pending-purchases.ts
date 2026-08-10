import { apiFetch } from "@/lib/api-client";
import type { OrderStatus } from "@/lib/types";

/** Libera cursos pagos no MP quando o webhook não chegou. Não deve travar a UI. */
export async function syncPendingPurchases(): Promise<OrderStatus[]> {
  try {
    return await Promise.race([
      apiFetch<OrderStatus[]>("/checkout/orders/sync-pending", { method: "POST" }),
      new Promise<OrderStatus[]>((resolve) => {
        window.setTimeout(() => resolve([]), 8000);
      }),
    ]);
  } catch {
    return [];
  }
}
