"use client";

import Link from "next/link";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";
import { Logo } from "@/components/logo";

/**
 * Cabeçalho usado apenas nas páginas públicas (landing, login, registro).
 * Usuários autenticados navegam pela sidebar do DashboardShell (components/layout/dashboard-shell.tsx),
 * então este cabeçalho não é renderizado quando há sessão ativa.
 */
export function SiteHeader() {
  const { user, isLoading } = useAuth();

  if (isLoading || user) {
    return null;
  }

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-background">
      <div className="mx-auto flex h-[4.5rem] max-w-6xl items-center justify-between px-5 sm:px-8">
        <Link href="/" aria-label="Página inicial — PKS Consultoria">
          <Logo size="md" priority />
        </Link>
        <nav className="flex items-center gap-2">
          <Link href="/login">
            <Button size="sm" variant="ghost">
              Entrar
            </Button>
          </Link>
          <Link href="/register">
            <Button size="sm">Criar conta</Button>
          </Link>
        </nav>
      </div>
    </header>
  );
}
