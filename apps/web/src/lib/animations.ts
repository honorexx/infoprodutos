import type { Transition, Variants } from "framer-motion";

/**
 * Animações reutilizáveis do sistema visual "Infoprodutos".
 *
 * Uso moderado, funcional (nunca decorativo/infinito). O respeito a
 * `prefers-reduced-motion` é aplicado globalmente via `MotionConfig`
 * (ver `src/app/layout.tsx`), então os componentes que usam estas variants
 * não precisam tratar isso individualmente.
 */

const easeOut: Transition["ease"] = [0.22, 1, 0.36, 1];

/** Entrada de página/seção: fade + leve deslocamento vertical. */
export const fadeIn: Variants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { duration: 0.3, ease: easeOut },
  },
};

export const slideUp: Variants = {
  hidden: { opacity: 0, y: 16 },
  visible: {
    opacity: 1,
    y: 0,
    transition: { duration: 0.35, ease: easeOut },
  },
};

/** Container para animar filhos em sequência curta (métricas, listas). */
export const staggerContainer: Variants = {
  hidden: {},
  visible: {
    transition: {
      staggerChildren: 0.06,
    },
  },
};

export const staggerItem: Variants = {
  hidden: {
    opacity: 0,
    y: 10,
  },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.35,
      ease: [0.22, 1, 0.36, 1],
    },
  },
};

/** Largura da sidebar ao expandir/recolher. */
export const sidebarTransition: Transition = {
  duration: 0.2,
  ease: easeOut,
};

/** Expandir/recolher um módulo na árvore de curso. */
export const expandCollapse: Variants = {
  collapsed: {
    height: 0,
    opacity: 0,
  },
  expanded: {
    height: "auto",
    opacity: 1,
    transition: {
      duration: 0.25,
      ease: [0.22, 1, 0.36, 1],
    },
  },
};

/** Preenchimento de barras de progresso — anima somente quando o valor muda. */
export const progressTransition: Transition = {
  duration: 0.6,
  ease: easeOut,
};
