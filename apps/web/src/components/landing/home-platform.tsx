"use client";

import { Reveal } from "@/components/landing/reveal";

/**
 * Prévia estrutural do LMS — chrome de interface, sem métricas inventadas.
 */
export function HomePlatform() {
  return (
    <section className="overflow-hidden bg-navy-950">
      <div className="mx-auto max-w-7xl px-5 py-24 sm:px-8 lg:px-10 lg:py-32">
        <div className="mb-14 max-w-xl">
          <Reveal>
            <p className="text-[0.6875rem] font-medium tracking-[0.22em] text-primary uppercase">
              Experiência na plataforma
            </p>
          </Reveal>
          <Reveal delay={0.05}>
            <h2 className="mt-6 text-balance font-heading text-3xl leading-[1.12] font-medium tracking-[-0.02em] text-foreground sm:text-4xl">
              Acompanhe o progresso
              <br />
              com clareza.
            </h2>
          </Reveal>
          <Reveal delay={0.1}>
            <p className="mt-4 text-sm leading-relaxed text-muted-foreground">
              Módulos, aulas e percentual reais — a partir das suas matrículas.
            </p>
          </Reveal>
        </div>

        <Reveal delay={0.08}>
          <div className="perspective-[1400px]">
            <div className="mx-auto max-w-4xl origin-center border border-white/[0.08] bg-navy-850 shadow-[0_40px_80px_-40px_rgba(0,0,0,0.55)] transition-transform duration-500 lg:[transform:rotateX(4deg)_rotateY(-4deg)]">
              <div className="flex items-center gap-2 border-b border-white/8 px-4 py-3">
                <span className="size-2 rounded-full bg-white/15" />
                <span className="size-2 rounded-full bg-white/15" />
                <span className="size-2 rounded-full bg-white/15" />
                <span className="ml-3 font-mono text-[0.625rem] tracking-[0.12em] text-slate-400 uppercase">
                  PKS · Área do aluno
                </span>
              </div>

              <div className="grid sm:grid-cols-[200px_1fr]">
                <aside className="hidden border-r border-white/8 bg-navy-800/80 p-5 sm:block">
                  <p className="text-[0.625rem] font-medium tracking-[0.16em] text-primary uppercase">
                    Módulos
                  </p>
                  <ul className="mt-5 space-y-3">
                    {["Módulo 01", "Módulo 02", "Módulo 03", "Módulo 04"].map((m, i) => (
                      <li
                        key={m}
                        className={
                          i === 2
                            ? "border-l-2 border-primary pl-3 text-sm text-foreground"
                            : "border-l-2 border-transparent pl-3 text-sm text-muted-foreground"
                        }
                      >
                        {m}
                      </li>
                    ))}
                  </ul>
                </aside>

                <div className="p-5 sm:p-8">
                  <p className="text-[0.625rem] font-medium tracking-[0.16em] text-primary uppercase">
                    Curso em andamento
                  </p>
                  <h3 className="mt-3 font-heading text-xl font-medium tracking-[-0.015em] text-foreground sm:text-2xl">
                    Sua formação
                  </h3>

                  <div className="mt-6">
                    <div className="flex items-baseline justify-between gap-4">
                      <span className="text-sm text-muted-foreground">Progresso</span>
                      <span className="font-mono text-sm text-primary">—</span>
                    </div>
                    <div className="mt-2 h-px w-full bg-white/8">
                      <div className="h-px w-0 bg-primary" />
                    </div>
                  </div>

                  <div className="mt-10 border-t border-white/8 pt-6">
                    <p className="text-[0.625rem] font-medium tracking-[0.14em] text-slate-400 uppercase">
                      Próxima aula
                    </p>
                    <p className="mt-2 text-base font-medium text-foreground">
                      Continuar de onde parou
                    </p>
                    <p className="mt-1 text-sm text-muted-foreground">
                      Dados reais após o login
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
