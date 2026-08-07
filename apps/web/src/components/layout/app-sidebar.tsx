"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion } from "framer-motion";
import { ChevronsLeft, ChevronsRight } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { cn } from "@/lib/utils";
import { sidebarTransition } from "@/lib/animations";
import { primaryNavigation, upcomingNavigation, filterNavByRole, type NavItem } from "@/config/navigation";
import { LogoMark } from "@/components/logo";
import { Badge } from "@/components/ui/badge";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

const ROLE_LABELS: Record<string, string> = {
  SUPER_ADMIN: "Administrador",
  INSTRUCTOR: "Professor",
  STUDENT: "Aluno",
};

/**
 * Sidebar fixa do desktop. A navegação mobile (drawer) vive em
 * `mobile-navigation.tsx` e compartilha os mesmos itens de `config/navigation.ts`.
 */
export function AppSidebar({
  collapsed,
  onToggleCollapsed,
}: {
  collapsed: boolean;
  onToggleCollapsed: () => void;
}) {
  const { user, hasRole } = useAuth();
  const pathname = usePathname();

  if (!user) return null;

  const primary = filterNavByRole(primaryNavigation, hasRole);
  const upcoming = filterNavByRole(upcomingNavigation, hasRole);

  const initials = user.name
    .split(" ")
    .map((part) => part[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

  return (
    <motion.aside
      initial={false}
      animate={{ width: collapsed ? 68 : 248 }}
      transition={sidebarTransition}
      className="sticky top-0 hidden h-screen shrink-0 flex-col overflow-hidden border-r border-sidebar-border bg-sidebar text-sidebar-foreground md:flex"
    >
      <div className={cn("flex h-16 items-center gap-2.5 px-4", collapsed && "justify-center px-0")}>
        <span className="flex size-8 shrink-0 items-center justify-center rounded-md bg-sidebar-accent text-sidebar-primary">
          <LogoMark className="size-4" />
        </span>
        {!collapsed && (
          <span className="whitespace-nowrap font-serif text-[1.1rem] leading-none font-medium tracking-tight italic">
            Infoprodutos
          </span>
        )}
      </div>

      <nav className="flex flex-1 flex-col gap-4 overflow-y-auto px-3 py-2">
        <div className="flex flex-col gap-0.5">
          {primary.map((item) => (
            <SidebarLink
              key={item.href}
              item={item}
              collapsed={collapsed}
              active={pathname === item.href || pathname.startsWith(item.href + "/")}
            />
          ))}
        </div>

        {upcoming.length > 0 && (
          <div className="flex flex-col gap-0.5">
            {!collapsed && (
              <p className="px-2.5 pb-1 text-[10px] font-semibold tracking-[0.14em] text-sidebar-muted uppercase">
                Próximas fases
              </p>
            )}
            {upcoming.map((item) => (
              <SidebarLink key={item.label} item={item} collapsed={collapsed} active={false} />
            ))}
          </div>
        )}
      </nav>

      <button
        type="button"
        onClick={onToggleCollapsed}
        className={cn(
          "mx-3 mb-1 flex items-center gap-2 rounded-md px-2.5 py-2 text-xs font-medium text-sidebar-muted transition-colors hover:bg-sidebar-accent/60 hover:text-sidebar-accent-foreground",
          collapsed && "justify-center px-0",
        )}
        aria-label={collapsed ? "Expandir menu" : "Recolher menu"}
      >
        {collapsed ? <ChevronsRight className="size-4" /> : <ChevronsLeft className="size-4" />}
        {!collapsed && "Recolher menu"}
      </button>

      <div className="border-t border-sidebar-border p-3">
        <div className={cn("flex items-center gap-2.5 rounded-md px-1 py-1.5", collapsed && "justify-center px-0")}>
          <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-sidebar-accent text-xs font-semibold text-sidebar-accent-foreground">
            {initials}
          </span>
          {!collapsed && (
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-medium">{user.name}</p>
              <p className="truncate text-xs text-sidebar-muted">
                {user.roles.map((r) => ROLE_LABELS[r] ?? r).join(" · ")}
              </p>
            </div>
          )}
        </div>
      </div>
    </motion.aside>
  );
}

function SidebarLink({
  item,
  active,
  collapsed,
}: {
  item: NavItem;
  active: boolean;
  collapsed: boolean;
}) {
  const Icon = item.icon;

  const content = item.comingSoon ? (
    <span
      className={cn(
        "flex items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium text-sidebar-foreground/40",
        collapsed && "justify-center px-0",
      )}
      aria-disabled="true"
    >
      <Icon className="size-4 shrink-0" />
      {!collapsed && (
        <span className="flex min-w-0 flex-1 items-center justify-between gap-2">
          <span className="truncate">{item.label}</span>
          <Badge variant="secondary" className="shrink-0 bg-sidebar-accent/50 text-sidebar-foreground/50">
            Em breve
          </Badge>
        </span>
      )}
    </span>
  ) : (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "flex items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium transition-colors",
        collapsed && "justify-center px-0",
        active
          ? "bg-sidebar-accent text-sidebar-accent-foreground"
          : "text-sidebar-foreground/75 hover:bg-sidebar-accent/50 hover:text-sidebar-accent-foreground",
      )}
    >
      <Icon className={cn("size-4 shrink-0", active && "text-sidebar-primary")} />
      {!collapsed && <span className="truncate">{item.label}</span>}
    </Link>
  );

  if (collapsed) {
    return (
      <Tooltip>
        <TooltipTrigger asChild>{content}</TooltipTrigger>
        <TooltipContent side="right">
          {item.label}
          {item.comingSoon ? " · Em breve" : ""}
        </TooltipContent>
      </Tooltip>
    );
  }

  return content;
}
