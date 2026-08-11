/** Suporte WhatsApp (PKS) — número com DDI Brasil. */
export const SUPPORT_WHATSAPP = {
  /** Dígitos com país: +55 41 98480-1999 */
  e164Digits: "5541984801999",
  display: "(41) 98480-1999",
  get href() {
    const text = encodeURIComponent(
      "Olá! Preciso de ajuda com a plataforma PKS Consultoria.",
    );
    return `https://wa.me/${this.e164Digits}?text=${text}`;
  },
} as const;
