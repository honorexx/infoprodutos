"use client";

import { GoldExpandLine, Reveal } from "@/components/landing/reveal";

export function HomeManifesto() {
  return (
    <section id="sobre" className="scroll-mt-20 bg-navy-950">
      <div className="mx-auto max-w-7xl px-5 py-28 sm:px-8 sm:py-36 lg:px-10 lg:py-44">
        <div className="mx-auto max-w-2xl">
          <Reveal>
            <p className="text-[0.6875rem] font-medium tracking-[0.22em] text-primary uppercase">
              Nossa visão
            </p>
          </Reveal>
          <Reveal delay={0.06}>
            <h2 className="mt-8 text-balance font-heading text-3xl leading-[1.15] font-medium tracking-[-0.02em] text-foreground sm:text-4xl lg:text-[2.75rem]">
              Não vendemos atalhos.
              <br />
              Ensinamos processo.
            </h2>
          </Reveal>
          <Reveal delay={0.12}>
            <GoldExpandLine className="mt-10 w-12" />
          </Reveal>
          <Reveal delay={0.16}>
            <p className="mt-10 max-w-xl text-pretty text-base leading-[1.75] text-muted-foreground sm:text-[1.0625rem]">
              A PKS transforma experiência prática, estratégia e conhecimento em
              formações estruturadas — com método claro, aplicação real e ritmo
              de quem opera no mercado. Sem fórmulas mágicas: clareza,
              disciplina e execução.
            </p>
          </Reveal>
        </div>
      </div>
    </section>
  );
}
