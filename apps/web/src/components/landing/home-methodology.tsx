"use client";

import { PKS_PHOTOS, PKS_PHOTO_SIZES } from "@/lib/landing-assets";
import { EditorialPhoto } from "@/components/landing/editorial-photo";
import { GoldExpandLine, Reveal, RevealImage, Stagger, StaggerItem } from "@/components/landing/reveal";

const STEPS = [
  { n: "01", label: "Fundamentos" },
  { n: "02", label: "Estratégia" },
  { n: "03", label: "Execução" },
  { n: "04", label: "Análise" },
  { n: "05", label: "Evolução" },
] as const;

export function HomeMethodology() {
  const photo = PKS_PHOTOS.mercedes;

  return (
    <section id="metodologia" className="scroll-mt-20 bg-navy-950">
      <div className="mx-auto grid max-w-7xl gap-10 px-5 py-24 sm:px-8 lg:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)] lg:items-center lg:gap-16 lg:px-10 lg:py-32">
        <div className="relative mx-auto aspect-[3/4] w-full max-w-md lg:mx-0 lg:max-w-none">
          <RevealImage className="absolute inset-0 h-full w-full" delay={0.06}>
            <EditorialPhoto
              src={photo.src}
              alt={photo.alt}
              objectPosition={photo.objectPosition}
              sizes={PKS_PHOTO_SIZES.portrait}
              tone="none"
            />
          </RevealImage>
        </div>

        <div className="flex flex-col lg:py-8">
          <Reveal>
            <p className="text-[0.6875rem] font-medium tracking-[0.22em] text-primary uppercase">
              Metodologia PKS
            </p>
          </Reveal>
          <Reveal delay={0.05}>
            <h2 className="mt-6 text-balance font-heading text-3xl leading-[1.12] font-medium tracking-[-0.02em] text-foreground sm:text-4xl lg:text-[2.65rem]">
              Da experiência
              <br />
              para o método.
            </h2>
          </Reveal>
          <Reveal delay={0.1}>
            <p className="mt-6 max-w-md text-pretty text-[0.9375rem] leading-relaxed text-muted-foreground sm:text-base">
              Conteúdo estruturado em etapas, aulas, exercícios e aplicação
              prática — para transformar o que se vive no mercado em processo
              replicável.
            </p>
          </Reveal>
          <Reveal delay={0.12}>
            <GoldExpandLine className="mt-10 w-10" />
          </Reveal>

          <Stagger className="mt-8 flex max-w-sm flex-col">
            {STEPS.map((step) => (
              <StaggerItem key={step.n}>
                <div className="flex items-baseline gap-5 border-b border-white/8 py-4 last:border-b-0">
                  <span className="w-8 shrink-0 font-mono text-sm tracking-[0.08em] text-primary">
                    {step.n}
                  </span>
                  <span className="text-[0.9375rem] font-medium tracking-[0.02em] text-foreground">
                    {step.label}
                  </span>
                </div>
              </StaggerItem>
            ))}
          </Stagger>
        </div>
      </div>
    </section>
  );
}
