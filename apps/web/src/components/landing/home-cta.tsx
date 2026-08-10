"use client";

import Link from "next/link";
import { GoldExpandLine, Reveal } from "@/components/landing/reveal";

export function HomeCta() {
  return (
    <section id="contato" className="scroll-mt-20 border-t border-[rgba(186,147,100,0.22)] bg-navy-900">
      <div className="mx-auto max-w-7xl px-5 py-28 sm:px-8 lg:px-10 lg:py-36">
        <div className="max-w-xl">
          <Reveal>
            <p className="text-[0.6875rem] font-medium tracking-[0.22em] text-primary uppercase">
              PKS Consultoria
            </p>
          </Reveal>
          <Reveal delay={0.05}>
            <h2 className="mt-6 text-balance font-heading text-3xl leading-[1.12] font-medium tracking-[-0.02em] text-foreground sm:text-4xl lg:text-[2.75rem]">
              O próximo nível começa
              <br />
              com uma decisão.
            </h2>
          </Reveal>
          <Reveal delay={0.1}>
            <GoldExpandLine className="mt-10 w-12" />
          </Reveal>
          <Reveal delay={0.14}>
            <div className="mt-12 flex flex-wrap items-center gap-6">
              <Link
                href="/cursos"
                className="inline-flex h-11 items-center bg-primary px-5 text-[0.6875rem] font-semibold tracking-[0.14em] text-primary-foreground transition-colors hover:bg-primary-hover"
              >
                EXPLORAR FORMAÇÕES
              </Link>
              <Link
                href="/register"
                className="group inline-flex items-center gap-2 text-sm tracking-[0.04em] text-muted-foreground transition-colors hover:text-primary"
              >
                Falar com a equipe
                <span aria-hidden className="transition-transform duration-300 group-hover:translate-x-1">
                  →
                </span>
              </Link>
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}
