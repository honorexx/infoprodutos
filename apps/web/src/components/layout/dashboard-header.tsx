"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { ChevronDown, LogOut, Menu, Settings } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";
import { LogoMark } from "@/components/logo";
import { CourseSearch } from "@/components/layout/course-search";
import { NotificationBell } from "@/components/layout/notification-bell";
import { getInitials } from "@/lib/utils";
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

/** Header minimalista do LMS. */
export function DashboardHeader({ onOpenMobileNav }: { onOpenMobileNav: () => void }) {
  const { user, logout } = useAuth();
  const router = useRouter();

  if (!user) return null;

  const initials = getInitials(user.name);

  async function handleLogout() {
    await logout();
    router.push("/login");
  }

  return (
    <header className="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-2 border-b border-border bg-background/90 px-3 backdrop-blur-[6px] sm:gap-3 sm:px-6">
      <Button
        variant="ghost"
        size="icon"
        className="md:hidden"
        onClick={onOpenMobileNav}
        aria-label="Abrir menu"
      >
        <Menu className="size-5" />
      </Button>
      <Link href="/dashboard" className="shrink-0 md:hidden" aria-label="PKS Consultoria">
        <LogoMark variant="gold" className="size-8" />
      </Link>

      <CourseSearch className="max-w-sm flex-1" />

      <div className="ml-auto flex shrink-0 items-center gap-1">
        <NotificationBell />

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="h-9 gap-2 px-2">
              <span className="flex size-7 shrink-0 items-center justify-center rounded-full bg-primary-soft text-[11px] font-semibold text-primary-soft-foreground">
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
              <span className="block truncate text-xs font-normal text-muted-foreground">
                {user.email}
              </span>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
              <Link href="/settings" className="gap-2">
                <Settings className="size-4" /> Configurações
              </Link>
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
