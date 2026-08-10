"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { Menu, X } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { cn } from "@/lib/utils";

const AUTH_PATHS = new Set(["/login", "/register"]);
const LOGO_GOLD = "/brand/pks-logo.png";

const NAV = [
  { href: "/", label: "Home" },
  { href: "/#sobre", label: "Sobre" },
  { href: "/cursos", label: "Cursos" },
  { href: "/#metodologia", label: "Metodologia" },
  { href: "/#consultoria", label: "Consultoria" },
  { href: "/#contato", label: "Contato" },
] as const;

/**
 * Cabeçalho das páginas públicas (landing).
 * Em /login e /register some — o AuthSplitLayout já é a composição completa.
 * Usuários autenticados usam o shell do dashboard.
 */
export function SiteHeader() {
  const { user } = useAuth();
  const pathname = usePathname();
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  // Login/register têm layout próprio; painel autenticado usa o shell —
  // exceto vitrine pública /cursos, que fica acessível logado ou não.
  if (AUTH_PATHS.has(pathname)) {
    return null;
  }
  if (user && pathname !== "/cursos" && !pathname.startsWith("/cursos/")) {
    return null;
  }

  return (
    <header
      className={cn(
        "fixed inset-x-0 top-0 z-50 transition-[background-color,border-color,backdrop-filter] duration-300",
        scrolled
          ? "border-b border-[rgba(186,147,100,0.15)] bg-[rgba(4,10,22,0.94)] backdrop-blur-[6px]"
          : "border-b border-transparent bg-transparent",
      )}
    >
      <div className="mx-auto flex h-14 max-w-7xl items-center justify-between gap-4 px-5 sm:px-8 lg:px-10">
        <Link
          href="/"
          aria-label="Página inicial — PKS Consultoria"
          className="inline-flex shrink-0 items-center"
        >
          <Image
            src={LOGO_GOLD}
            alt="PKS Consultoria"
            width={160}
            height={42}
            sizes="160px"
            quality={100}
            priority
            unoptimized
            className="h-7 w-auto object-contain object-left sm:h-8"
          />
        </Link>

        <nav
          className="absolute left-1/2 hidden -translate-x-1/2 items-center gap-7 lg:flex"
          aria-label="Principal"
        >
          {NAV.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className="group relative text-[0.8125rem] font-medium tracking-[0.04em] text-foreground/85 transition-colors hover:text-foreground"
            >
              {item.label}
              <span
                aria-hidden
                className="absolute -bottom-1 left-0 h-px w-full origin-left scale-x-0 bg-primary transition-transform duration-300 group-hover:scale-x-100"
              />
            </Link>
          ))}
        </nav>

        <div className="hidden items-center gap-2 sm:flex lg:gap-3">
          {user ? (
            <Link
              href="/dashboard"
              className="inline-flex h-9 items-center bg-primary px-4 text-[0.6875rem] font-semibold tracking-[0.12em] text-primary-foreground transition-colors hover:bg-primary-hover"
            >
              ÁREA LOGADA
            </Link>
          ) : (
            <>
              <Link
                href="/login"
                className="px-3 py-1.5 text-[0.8125rem] font-medium tracking-[0.04em] text-muted-foreground transition-colors hover:text-foreground"
              >
                Entrar
              </Link>
              <Link
                href="/cursos"
                className="inline-flex h-9 items-center bg-primary px-4 text-[0.6875rem] font-semibold tracking-[0.12em] text-primary-foreground transition-colors hover:bg-primary-hover"
              >
                VER CURSOS
              </Link>
            </>
          )}
        </div>

        <button
          type="button"
          className="inline-flex size-9 items-center justify-center text-foreground lg:hidden"
          aria-expanded={open}
          aria-controls="mobile-nav"
          aria-label={open ? "Fechar menu" : "Abrir menu"}
          onClick={() => setOpen((v) => !v)}
        >
          {open ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
      </div>

      {open && (
        <div
          id="mobile-nav"
          className="border-t border-[rgba(186,147,100,0.12)] bg-[rgba(4,10,22,0.98)] lg:hidden"
        >
          <nav className="mx-auto flex max-w-7xl flex-col gap-1 px-5 py-4 sm:px-8" aria-label="Mobile">
            {NAV.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setOpen(false)}
                className="py-2.5 text-sm font-medium tracking-[0.04em] text-foreground/90"
              >
                {item.label}
              </Link>
            ))}
            <div className="mt-3 flex flex-col gap-2 border-t border-white/8 pt-4">
              {user ? (
                <Link
                  href="/dashboard"
                  onClick={() => setOpen(false)}
                  className="inline-flex h-10 items-center justify-center bg-primary text-[0.6875rem] font-semibold tracking-[0.12em] text-primary-foreground"
                >
                  ÁREA LOGADA
                </Link>
              ) : (
                <>
                  <Link
                    href="/login"
                    onClick={() => setOpen(false)}
                    className="py-2 text-sm text-muted-foreground"
                  >
                    Entrar
                  </Link>
                  <Link
                    href="/cursos"
                    onClick={() => setOpen(false)}
                    className="inline-flex h-10 items-center justify-center bg-primary text-[0.6875rem] font-semibold tracking-[0.12em] text-primary-foreground"
                  >
                    VER CURSOS
                  </Link>
                </>
              )}
            </div>
          </nav>
        </div>
      )}
    </header>
  );
}
