import { apiFetch } from "@/lib/api-client";
import type { OrderStatus } from "@/lib/types";

/** Libera cursos pagos no MP quando o webhook não chegou (ex.: localhost). */
export async function syncPendingPurchases(): Promise<OrderStatus[]> {
  try {
    return await apiFetch<OrderStatus[]>("/checkout/orders/sync-pending", { method: "POST" });
  } catch {
    return [];
  }
}
