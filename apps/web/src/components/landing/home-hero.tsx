"use client";

import Link from "next/link";
import { PKS_PHOTOS, PKS_PHOTO_SIZES } from "@/lib/landing-assets";
import { EditorialPhoto } from "@/components/landing/editorial-photo";

/** Hero editorial sem animação de opacity — sempre legível (localhost / ngrok). */
export function HomeHero() {
  const photo = PKS_PHOTOS.rooftop;

  return (
    <section className="relative overflow-hidden bg-navy-950">
      <div className="mx-auto grid max-w-7xl lg:min-h-[100svh] lg:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)]">
        <div className="relative z-10 flex flex-col justify-center px-5 pb-8 pt-24 sm:px-8 sm:pt-28 lg:px-10 lg:pb-16 lg:pt-20">
          <div className="flex max-w-xl flex-col">
            <p className="text-[0.6875rem] font-medium tracking-[0.22em] text-primary uppercase">
              Estratégia · Conhecimento · Execução
            </p>
            <h1 className="mt-6 text-balance font-heading text-[2.35rem] leading-[1.08] font-medium tracking-[-0.02em] text-foreground sm:text-5xl lg:text-[3.35rem]">
              Conhecimento para quem
              <br className="hidden sm:block" />{" "}
              quer jogar em{" "}
              <span className="text-primary italic">outro nível.</span>
            </h1>
            <p className="mt-6 max-w-md text-pretty text-[0.9375rem] leading-relaxed text-muted-foreground sm:text-base">
              Estratégia, marketing e experiência prática transformados em
              conhecimento aplicável para quem quer evoluir profissionalmente e
              construir resultados consistentes.
            </p>
            <div className="mt-9 flex flex-wrap items-center gap-3">
              <Link
                href="/cursos"
                className="inline-flex h-11 items-center bg-primary px-5 text-[0.6875rem] font-semibold tracking-[0.14em] text-primary-foreground transition-colors hover:bg-primary-hover"
              >
                CONHECER OS CURSOS
              </Link>
              <Link
                href="/#sobre"
                className="inline-flex h-11 items-center border border-white/12 px-5 text-[0.6875rem] font-semibold tracking-[0.14em] text-foreground transition-colors hover:border-primary/40 hover:bg-navy-800"
              >
                CONHECER A PKS
              </Link>
            </div>
            <div className="mt-12 flex flex-col gap-4 lg:mt-14">
              <span aria-hidden className="block h-px w-16 bg-primary" />
              <p className="max-w-sm text-[0.75rem] leading-relaxed tracking-[0.02em] text-slate-400">
                Formações estruturadas com processo, método e aplicação — para quem
                decide operar com mais clareza.
              </p>
            </div>
          </div>
        </div>

        <div className="relative mt-4 h-[72vw] max-h-[480px] min-h-[300px] w-full sm:mt-6 lg:mt-0 lg:h-auto lg:max-h-none lg:min-h-[100svh]">
          <div className="absolute inset-0 h-full w-full">
            <EditorialPhoto
              src={photo.src}
              alt={photo.alt}
              objectPosition={photo.objectPosition}
              sizes={PKS_PHOTO_SIZES.hero}
              priority
              tone="none"
            />
          </div>
          <div
            aria-hidden
            className="pointer-events-none absolute inset-y-0 left-0 hidden w-[45%] lg:block"
            style={{
              background:
                "linear-gradient(to right, #040A16 0%, rgba(4,10,22,.80) 18%, rgba(4,10,22,.10) 55%, transparent 100%)",
            }}
          />
          <div
            aria-hidden
            className="pointer-events-none absolute inset-x-0 top-0 h-16 bg-gradient-to-b from-navy-950/80 to-transparent lg:hidden"
          />
        </div>
      </div>
    </section>
  );
}
