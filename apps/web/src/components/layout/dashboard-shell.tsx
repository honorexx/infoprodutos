"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/lib/auth-context";
import { AppSidebar } from "@/components/layout/app-sidebar";
import { MobileNavigation } from "@/components/layout/mobile-navigation";
import { DashboardHeader } from "@/components/layout/dashboard-header";

const SIDEBAR_COLLAPSE_KEY = "infoprodutos:sidebar-collapsed";

/**
 * Composição estrutural da área autenticada: sidebar (desktop), drawer
 * (mobile) e cabeçalho persistente. Usado tanto pelo grupo de rotas
 * `(dashboard)` quanto diretamente por páginas fora do grupo (cursos,
 * administração) para manter a mesma identidade visual em toda a aplicação.
 */
export function DashboardShell({ children }: { children: React.ReactNode }) {
  const { user, logout } = useAuth();
  const router = useRouter();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  useEffect(() => {
    // Sincroniza com a preferência salva do usuário (localStorage é externo ao React).
    setCollapsed(window.localStorage.getItem(SIDEBAR_COLLAPSE_KEY) === "1");
  }, []);

  function toggleCollapsed() {
    setCollapsed((prev) => {
      window.localStorage.setItem(SIDEBAR_COLLAPSE_KEY, prev ? "0" : "1");
      return !prev;
    });
  }

  async function handleLogout() {
    await logout();
    router.push("/login");
  }

  if (!user) {
    return <>{children}</>;
  }

  return (
    <div className="flex min-h-screen bg-background">
      <AppSidebar collapsed={collapsed} onToggleCollapsed={toggleCollapsed} />
      <MobileNavigation open={mobileOpen} onClose={() => setMobileOpen(false)} onLogout={handleLogout} />

      <div className="flex min-w-0 flex-1 flex-col">
        <DashboardHeader onOpenMobileNav={() => setMobileOpen(true)} />
        <main className="flex flex-1 flex-col">{children}</main>
      </div>
    </div>
  );
}
