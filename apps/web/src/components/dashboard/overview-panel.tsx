"use client";

import { motion } from "framer-motion";
import { ArrowDownRight, ArrowUpRight } from "lucide-react";
import { AppCard } from "@/components/ui/app-card";
import { staggerContainer, staggerItem } from "@/lib/animations";
import type { Metric } from "@/types/dashboard";

function Sparkline({ points }: { points: number[] }) {
  const max = Math.max(...points);
  const min = Math.min(...points);
  const range = max - min || 1;
  const coords = points
    .map((p, i) => {
      const x = (i / (points.length - 1)) * 100;
      const y = 24 - ((p - min) / range) * 24;
      return `${x},${y}`;
    })
    .join(" ");

  return (
    <svg viewBox="0 0 100 24" preserveAspectRatio="none" className="h-6 w-16 shrink-0" aria-hidden="true">
      <polyline points={coords} fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

/**
 * Painel principal escuro do dashboard — reúne as métricas de maior
 * destaque. Dados vêm de `src/mocks/dashboard.ts` (demonstração).
 */
export function OverviewPanel({ metrics }: { metrics: Metric[] }) {
  return (
    <AppCard tone="inverse" className="p-6 sm:p-7">
      <div className="mb-6 flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-semibold tracking-[0.14em] text-sidebar-muted uppercase">Visão geral</p>
          <p className="mt-1 text-sm text-sidebar-foreground/70">Dados de demonstração</p>
        </div>
      </div>

      <motion.div
        variants={staggerContainer}
        initial="hidden"
        animate="visible"
        className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4"
      >
        {metrics.map((metric) => {
          const Icon = metric.icon;
          const positive = (metric.change ?? 0) >= 0;
          return (
            <motion.div key={metric.id} variants={staggerItem} className="flex flex-col gap-3">
              <div className="flex items-center justify-between">
                <span className="flex size-9 items-center justify-center rounded-md bg-sidebar-accent text-sidebar-primary">
                  <Icon className="size-4.5" />
                </span>
                {metric.trend && <Sparkline points={metric.trend} />}
              </div>
              <div>
                <p className="font-serif text-3xl leading-none font-medium tracking-tight">{metric.value}</p>
                <p className="mt-1.5 text-xs text-sidebar-foreground/60">{metric.label}</p>
              </div>
              {typeof metric.change === "number" && (
                <span
                  className={`inline-flex w-fit items-center gap-1 text-xs font-medium ${
                    positive ? "text-sidebar-primary" : "text-danger"
                  }`}
                >
                  {positive ? <ArrowUpRight className="size-3.5" /> : <ArrowDownRight className="size-3.5" />}
                  {Math.abs(metric.change)}%
                </span>
              )}
            </motion.div>
          );
        })}
      </motion.div>
    </AppCard>
  );
}
