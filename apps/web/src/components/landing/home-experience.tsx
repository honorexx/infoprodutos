"use client";

import { PKS_PHOTOS, PKS_PHOTO_SIZES } from "@/lib/landing-assets";
import { EditorialPhoto } from "@/components/landing/editorial-photo";
import { GoldExpandLine, Reveal, RevealImage, Stagger, StaggerItem } from "@/components/landing/reveal";

const TOPICS = ["Estratégia", "Execução", "Análise"] as const;

export function HomeExperience() {
  const photo = PKS_PHOTOS.bmw;

  return (
    <section id="consultoria" className="scroll-mt-20 bg-navy-900">
      <div className="mx-auto grid max-w-7xl lg:grid-cols-2 lg:items-stretch">
        <div className="flex flex-col justify-center px-5 py-20 sm:px-8 lg:order-2 lg:px-14 lg:py-28 xl:px-20">
          <Reveal>
            <p className="text-[0.6875rem] font-medium tracking-[0.22em] text-primary uppercase">
              Experiência real
            </p>
          </Reveal>
          <Reveal delay={0.05}>
            <h2 className="mt-6 text-balance font-heading text-3xl leading-[1.12] font-medium tracking-[-0.02em] text-foreground sm:text-4xl">
              Conteúdo criado por quem
              <br className="hidden sm:block" />{" "}
              vive o mercado.
            </h2>
          </Reveal>
          <Reveal delay={0.1}>
            <p className="mt-6 max-w-md text-pretty text-[0.9375rem] leading-relaxed text-muted-foreground sm:text-base">
              A proposta da PKS é transformar experiência, erros, decisões e
              estratégias reais em conhecimento estruturado e aplicável.
            </p>
          </Reveal>
          <Reveal delay={0.14}>
            <GoldExpandLine className="mt-10 w-10" />
          </Reveal>
          <Stagger className="mt-10 flex flex-col gap-0">
            {TOPICS.map((topic, i) => (
              <StaggerItem key={topic}>
                <div className="flex items-baseline gap-4 border-t border-white/8 py-4 first:border-t-0">
                  <span className="font-mono text-[0.6875rem] tracking-[0.12em] text-primary">
                    {String(i + 1).padStart(2, "0")}
                  </span>
                  <span className="text-sm font-medium tracking-[0.06em] text-foreground">
                    {topic}
                  </span>
                </div>
              </StaggerItem>
            ))}
          </Stagger>
        </div>

        <div className="relative aspect-[4/5] w-full sm:aspect-[3/4] lg:order-1 lg:aspect-auto lg:min-h-[640px]">
          <RevealImage className="absolute inset-0 h-full w-full" delay={0.08}>
            <EditorialPhoto
              src={photo.src}
              alt={photo.alt}
              objectPosition={photo.objectPosition}
              sizes={PKS_PHOTO_SIZES.split}
              tone="none"
            />
          </RevealImage>
          <div
            aria-hidden
            className="pointer-events-none absolute inset-y-0 right-0 hidden w-24 bg-gradient-to-l from-navy-900 to-transparent lg:block"
          />
        </div>
      </div>
    </section>
  );
}
