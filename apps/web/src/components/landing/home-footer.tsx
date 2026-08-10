"use client";

import Link from "next/link";
import Image from "next/image";

const MONO_GOLD = "/brand/pks-monogram.png";

const LINKS = [
  { href: "/#sobre", label: "Sobre" },
  { href: "/#metodologia", label: "Metodologia" },
  { href: "/cursos", label: "Cursos" },
  { href: "/login", label: "Entrar" },
] as const;

export function HomeFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="bg-navy-950">
      <div className="mx-auto max-w-7xl px-5 pt-24 pb-12 sm:px-8 lg:px-10 lg:pt-32">
        <div className="flex flex-col gap-16 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <Link href="/" aria-label="PKS Consultoria">
              <Image
                src={MONO_GOLD}
                alt="PKS Consultoria"
                width={140}
                height={71}
                sizes="140px"
                unoptimized
                className="h-8 w-auto object-contain"
              />
            </Link>
            <p className="mt-6 max-w-xs text-sm leading-relaxed text-muted-foreground">
              Estratégia, conhecimento e execução para quem quer operar em outro
              nível.
            </p>
          </div>

          <nav aria-label="Rodapé" className="flex flex-wrap gap-x-10 gap-y-3">
            {LINKS.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className="text-sm tracking-[0.04em] text-muted-foreground transition-colors hover:text-primary"
              >
                {item.label}
              </Link>
            ))}
          </nav>
        </div>

        <div className="mt-24 flex flex-col gap-4 border-t border-white/[0.06] pt-8 sm:flex-row sm:items-center sm:justify-between">
          <p className="text-[0.75rem] tracking-[0.04em] text-slate-400">
            © {year} PKS Consultoria. Todos os direitos reservados.
          </p>
          <Link
            href="/#contato"
            className="text-[0.75rem] tracking-[0.04em] text-slate-400 transition-colors hover:text-primary"
          >
            Contato
          </Link>
        </div>
      </div>
    </footer>
  );
}
