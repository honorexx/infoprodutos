"use client";

import { motion } from "framer-motion";
import { GoldExpandLine, Reveal, Stagger, StaggerItem } from "@/components/landing/reveal";

const SIGNALS = [
  { label: "Atividades", status: "Assistido" },
  { label: "Correção", status: "Em revisão" },
  { label: "Evolução", status: "Contínua" },
] as const;

export function HomeAi() {
  return (
    <section className="bg-navy-900">
      <div className="mx-auto grid max-w-7xl gap-14 px-5 py-24 sm:px-8 lg:grid-cols-[1.1fr_0.9fr] lg:items-center lg:gap-20 lg:px-10 lg:py-32">
        <div>
          <Reveal>
            <p className="text-[0.6875rem] font-medium tracking-[0.22em] text-primary uppercase">
              Plataforma
            </p>
          </Reveal>
          <Reveal delay={0.05}>
            <h2 className="mt-6 text-balance font-heading text-3xl leading-[1.12] font-medium tracking-[-0.02em] text-foreground sm:text-4xl lg:text-[2.65rem]">
              Tecnologia que acompanha
              <br />
              o seu aprendizado.
            </h2>
          </Reveal>
          <Reveal delay={0.1}>
            <p className="mt-6 max-w-lg text-pretty text-[0.9375rem] leading-relaxed text-muted-foreground sm:text-base">
              A plataforma utiliza inteligência artificial para auxiliar na
              criação e correção de atividades — sempre com revisão humana — e
              na evolução futura da experiência educacional.
            </p>
          </Reveal>
          <Reveal delay={0.14}>
            <GoldExpandLine className="mt-10 w-12" />
          </Reveal>
        </div>

        <Reveal delay={0.12}>
          <div className="relative border border-white/[0.08] bg-navy-850 p-6 sm:p-8">
            <div className="flex items-center justify-between border-b border-white/8 pb-4">
              <span className="font-mono text-[0.6875rem] tracking-[0.14em] text-primary uppercase">
                Processamento
              </span>
              <span className="flex items-center gap-2 text-[0.6875rem] tracking-[0.08em] text-slate-400">
                <span aria-hidden className="inline-block size-1.5 rounded-full bg-primary" />
                Ativo
              </span>
            </div>

            <Stagger className="mt-6 flex flex-col gap-0">
              {SIGNALS.map((row) => (
                <StaggerItem key={row.label}>
                  <div className="flex items-center justify-between border-b border-white/8 py-4 last:border-b-0">
                    <span className="text-sm text-foreground">{row.label}</span>
                    <span className="font-mono text-[0.6875rem] tracking-[0.1em] text-primary">
                      {row.status}
                    </span>
                  </div>
                </StaggerItem>
              ))}
            </Stagger>

            <div className="mt-8 space-y-2" aria-hidden>
              {[0.92, 0.64, 0.78].map((w, i) => (
                <motion.div
                  key={i}
                  className="h-px origin-left bg-primary/50"
                  initial={{ scaleX: 0 }}
                  whileInView={{ scaleX: w }}
                  viewport={{ once: true }}
                  transition={{ duration: 0.7, delay: 0.15 + i * 0.08, ease: [0.22, 1, 0.36, 1] }}
                />
              ))}
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
