"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion } from "framer-motion";
import { ChevronsLeft, ChevronsRight } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { cn, getInitials } from "@/lib/utils";
import { sidebarTransition } from "@/lib/animations";
import { primaryNavigation, filterNavByRole, type NavItem } from "@/config/navigation";
import { SidebarBrand } from "@/components/logo";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";

const ROLE_LABELS: Record<string, string> = {
  SUPER_ADMIN: "Administrador",
  INSTRUCTOR: "Professor",
  STUDENT: "Aluno",
};

/**
 * Sidebar navy do LMS. Item ativo: texto gold + bg soft + indicador lateral.
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
  const initials = getInitials(user.name);
  const roleLabel = user.roles.map((r) => ROLE_LABELS[r] ?? r)[0] ?? "Usuário";

  return (
    <motion.aside
      initial={false}
      animate={{ width: collapsed ? 72 : 248 }}
      transition={sidebarTransition}
      className="sticky top-0 hidden h-screen shrink-0 flex-col overflow-hidden border-r border-sidebar-border bg-sidebar text-sidebar-foreground md:flex"
    >
      <div
        className={cn(
          "flex h-14 items-center border-b border-sidebar-border",
          collapsed ? "justify-center px-0" : "justify-start px-4",
        )}
      >
        <SidebarBrand collapsed={collapsed} />
      </div>

      {!collapsed && (
        <div className="border-b border-sidebar-border px-4 py-4">
          <div className="flex items-center gap-3">
            <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary-soft text-xs font-semibold text-primary-soft-foreground">
              {initials}
            </span>
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-foreground">{user.name}</p>
              <p className="truncate text-xs text-muted-foreground">{roleLabel}</p>
            </div>
          </div>
        </div>
      )}

      <nav className="flex flex-1 flex-col gap-1 overflow-y-auto px-2 py-3" aria-label="Principal">
        {primary.map((item) => (
          <SidebarLink
            key={`${item.label}-${item.href}`}
            item={item}
            collapsed={collapsed}
            active={pathname === item.href || pathname.startsWith(`${item.href}/`)}
          />
        ))}
      </nav>

      <div className="mt-auto border-t border-sidebar-border px-2 py-3">
        <button
          type="button"
          onClick={onToggleCollapsed}
          className={cn(
            "flex w-full items-center gap-3 rounded-md px-3 py-2 text-xs font-medium text-muted-foreground transition-colors hover:bg-sidebar-item-hover hover:text-foreground",
            collapsed && "justify-center px-0",
          )}
          aria-label={collapsed ? "Expandir menu" : "Recolher menu"}
        >
          {collapsed ? <ChevronsRight className="size-4" /> : <ChevronsLeft className="size-4" />}
          {!collapsed && "Recolher"}
        </button>
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

  const link = (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "relative flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
        collapsed && "justify-center px-0",
        active
          ? "bg-sidebar-accent text-sidebar-accent-foreground"
          : "text-muted-foreground hover:bg-sidebar-item-hover hover:text-foreground",
      )}
    >
      {active && (
        <span
          aria-hidden
          className="absolute top-1.5 bottom-1.5 left-0 w-0.5 rounded-full bg-primary"
        />
      )}
      <Icon className={cn("size-4 shrink-0", active && "text-primary-hover")} />
      {!collapsed && <span className="truncate">{item.label}</span>}
    </Link>
  );

  if (collapsed) {
    return (
      <Tooltip>
        <TooltipTrigger asChild>{link}</TooltipTrigger>
        <TooltipContent side="right">{item.label}</TooltipContent>
      </Tooltip>
    );
  }

  return link;
}
