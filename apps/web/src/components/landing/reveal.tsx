"use client";

import { motion, type HTMLMotionProps } from "framer-motion";
import { cn } from "@/lib/utils";

const ease = [0.22, 1, 0.36, 1] as const;

const viewport = { once: true, amount: 0.15 as const, margin: "0px 0px -5% 0px" };

export function Reveal({
  children,
  className,
  delay = 0,
  y = 12,
  ...props
}: HTMLMotionProps<"div"> & { delay?: number; y?: number }) {
  return (
    <motion.div
      className={cn(className)}
      initial={{ opacity: 0, y }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={viewport}
      transition={{ duration: 0.45, delay, ease }}
      {...props}
    >
      {children}
    </motion.div>
  );
}

/** Entrada imediata (above the fold) — não depende de IntersectionObserver. */
export function RevealOnMount({
  children,
  className,
  delay = 0,
  y = 12,
}: {
  children: React.ReactNode;
  className?: string;
  delay?: number;
  y?: number;
}) {
  return (
    <motion.div
      className={cn(className)}
      initial={{ opacity: 0, y }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay, ease }}
    >
      {children}
    </motion.div>
  );
}

export function RevealImage({
  children,
  className,
  delay = 0.08,
  onMount = false,
}: {
  children: React.ReactNode;
  className?: string;
  delay?: number;
  onMount?: boolean;
}) {
  const motionProps = onMount
    ? {
        initial: { opacity: 0.35, clipPath: "inset(6% 3% 6% 3%)" },
        animate: { opacity: 1, clipPath: "inset(0% 0% 0% 0%)" },
      }
    : {
        initial: { opacity: 0.35, clipPath: "inset(6% 3% 6% 3%)" },
        whileInView: { opacity: 1, clipPath: "inset(0% 0% 0% 0%)" },
        viewport,
      };

  return (
    <motion.div
      className={cn("overflow-hidden", className)}
      transition={{ duration: 0.7, delay, ease }}
      {...motionProps}
    >
      {children}
    </motion.div>
  );
}

export function Stagger({
  children,
  className,
  onMount = false,
}: {
  children: React.ReactNode;
  className?: string;
  onMount?: boolean;
}) {
  const shared = {
    className: cn(className),
    variants: {
      hidden: {},
      visible: { transition: { staggerChildren: 0.07 } },
    },
    initial: "hidden" as const,
  };

  if (onMount) {
    return (
      <motion.div {...shared} animate="visible">
        {children}
      </motion.div>
    );
  }

  return (
    <motion.div {...shared} whileInView="visible" viewport={viewport}>
      {children}
    </motion.div>
  );
}

export function StaggerItem({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <motion.div
      className={cn(className)}
      variants={{
        hidden: { opacity: 0, y: 10 },
        visible: {
          opacity: 1,
          y: 0,
          transition: { duration: 0.4, ease },
        },
      }}
    >
      {children}
    </motion.div>
  );
}

export function GoldExpandLine({
  className,
  onMount = false,
}: {
  className?: string;
  onMount?: boolean;
}) {
  const props = onMount
    ? {
        initial: { scaleX: 0 },
        animate: { scaleX: 1 },
      }
    : {
        initial: { scaleX: 0 },
        whileInView: { scaleX: 1 },
        viewport: { once: true },
      };

  return (
    <motion.span
      aria-hidden
      className={cn("block h-px origin-left bg-primary", className)}
      transition={{ duration: 0.55, ease }}
      {...props}
    />
  );
}
