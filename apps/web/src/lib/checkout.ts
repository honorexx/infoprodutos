import { apiFetch } from "@/lib/api-client";
import type { CheckoutSession } from "@/lib/types";

export async function startCheckout(input: { courseId?: string; packageId?: string }) {
  const session = await apiFetch<CheckoutSession>("/checkout/sessions", {
    method: "POST",
    body: {
      courseId: input.courseId ?? null,
      packageId: input.packageId ?? null,
    },
  });

  if (typeof window !== "undefined") {
    window.sessionStorage.setItem("checkout:lastOrderId", session.orderId);
  }

  if (session.mockMode) {
    const allowMock = window.confirm(
      "A API está em modo mock (sem Mercado Pago).\n\n" +
        "Isso acontece se MP_ACCESS_TOKEN não foi carregado — confira apps/api/.env e reinicie a ApiApplication.\n\n" +
        "Liberar o curso sem pagar mesmo assim?",
    );
    if (!allowMock) {
      throw new Error("Checkout cancelado: configure o Mercado Pago e tente de novo.");
    }
    await apiFetch(`/checkout/orders/${session.orderId}/simulate-payment`, { method: "POST" });
    const returnUrl = new URL("/checkout/return", window.location.origin);
    returnUrl.searchParams.set("orderId", session.orderId);
    returnUrl.searchParams.set("status", "success");
    returnUrl.searchParams.set("mock", "1");
    window.location.assign(returnUrl.href);
    return session;
  }

  const url = session.initPoint || session.sandboxInitPoint;
  if (!url) {
    throw new Error("Checkout sem URL do Mercado Pago.");
  }
  // Em localhost o MP não redireciona de volta (só HTTPS). Após pagar, volte e abra Meus cursos.
  window.location.assign(url);
  return session;
}

export function loginUrlForCheckout(input: { courseId?: string; packageId?: string }) {
  const params = new URLSearchParams();
  if (input.courseId) params.set("courseId", input.courseId);
  if (input.packageId) params.set("packageId", input.packageId);
  const next = `/checkout?${params.toString()}`;
  return `/login?next=${encodeURIComponent(next)}`;
}
