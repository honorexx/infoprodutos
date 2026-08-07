"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { Bell, ChevronDown, LogOut, Menu, Search, Settings } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { mockNotifications } from "@/mocks/dashboard";
import { Button } from "@/components/ui/button";
import { LogoMark } from "@/components/logo";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const ROLE_LABELS: Record<string, string> = {
  SUPER_ADMIN: "Administrador",
  INSTRUCTOR: "Professor",
  STUDENT: "Aluno",
};

/**
 * Cabeçalho persistente da área autenticada: busca (visual, sem back-end),
 * notificações (dados de demonstração) e menu de perfil (logout real).
 */
export function DashboardHeader({ onOpenMobileNav }: { onOpenMobileNav: () => void }) {
  const { user, logout } = useAuth();
  const router = useRouter();

  if (!user) return null;

  const initials = user.name
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  async function handleLogout() {
    await logout();
    router.push("/login");
  }

  return (
    <header className="sticky top-0 z-30 flex h-16 shrink-0 items-center gap-3 border-b border-border/70 bg-background/85 px-4 backdrop-blur-sm sm:px-6">
      <Button variant="ghost" size="icon" className="md:hidden" onClick={onOpenMobileNav} aria-label="Abrir menu">
        <Menu className="size-5" />
      </Button>
      {/* Logo só no mobile — no desktop fica na sidebar dourada */}
      <Link href="/" className="shrink-0 md:hidden" aria-label="PKS Consultoria">
        <LogoMark variant="gold" className="size-9" />
      </Link>

      <label className="relative hidden max-w-sm flex-1 md:block">
        <span className="sr-only">Buscar</span>
        <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="search"
          placeholder="Buscar alunos, cursos, aulas…"
          className="h-9 w-full rounded-md border border-border bg-surface pr-12 pl-9 text-sm outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/25"
        />
        <kbd className="pointer-events-none absolute top-1/2 right-2.5 -translate-y-1/2 rounded border border-border bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
          ⌘K
        </kbd>
      </label>

      <div className="ml-auto flex items-center gap-1.5">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" aria-label="Notificações" className="relative">
              <Bell className="size-4.5" />
              <span className="absolute top-1.5 right-1.5 size-1.5 rounded-full bg-accent" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-72">
            <DropdownMenuLabel>Notificações</DropdownMenuLabel>
            <DropdownMenuSeparator />
            {mockNotifications.map((n) => (
              <DropdownMenuItem key={n.id} className="whitespace-normal text-sm">
                {n.text}
              </DropdownMenuItem>
            ))}
            <DropdownMenuSeparator />
            <p className="px-2 py-1.5 text-[11px] text-muted-foreground">Conteúdo de demonstração.</p>
          </DropdownMenuContent>
        </DropdownMenu>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="h-9 gap-2 px-2">
              <span className="flex size-6 shrink-0 items-center justify-center rounded-full bg-primary-soft text-[11px] font-semibold text-primary-soft-foreground">
                {initials}
              </span>
              <span className="hidden text-left leading-tight sm:block">
                <span className="block text-sm font-medium">{user.name.split(" ")[0]}</span>
                <span className="block text-[11px] text-muted-foreground">
                  {user.roles.map((r) => ROLE_LABELS[r] ?? r)[0]}
                </span>
              </span>
              <ChevronDown className="size-3.5 text-muted-foreground" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuLabel>
              <span className="block truncate font-medium">{user.name}</span>
              <span className="block truncate text-xs font-normal text-muted-foreground">{user.email}</span>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem disabled className="gap-2">
              <Settings className="size-4" /> Configurações
            </DropdownMenuItem>
            <DropdownMenuItem onSelect={handleLogout} className="gap-2 text-danger focus:text-danger">
              <LogOut className="size-4" /> Sair
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
