"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import { X, LogOut } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { cn, getInitials } from "@/lib/utils";
import { primaryNavigation, filterNavByRole } from "@/config/navigation";
import { SidebarBrand } from "@/components/logo";
import { Button } from "@/components/ui/button";

/** Drawer mobile — mesmos itens e tokens da sidebar navy. */
export function MobileNavigation({
  open,
  onClose,
  onLogout,
}: {
  open: boolean;
  onClose: () => void;
  onLogout: () => void;
}) {
  const { user, hasRole } = useAuth();
  const pathname = usePathname();

  if (!user) return null;

  const primary = filterNavByRole(primaryNavigation, hasRole);
  const initials = getInitials(user.name);

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-50 flex md:hidden">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.18 }}
            className="absolute inset-0 bg-navy-950/75"
            onClick={onClose}
            aria-hidden
          />
          <motion.aside
            initial={{ x: -300 }}
            animate={{ x: 0 }}
            exit={{ x: -300 }}
            transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
            className="relative flex w-[min(300px,88vw)] flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground"
            role="dialog"
            aria-modal="true"
            aria-label="Menu"
          >
            <div className="flex items-center justify-between gap-2 border-b border-sidebar-border px-4 py-3.5">
              <SidebarBrand />
              <Button
                variant="ghost"
                size="icon-sm"
                onClick={onClose}
                aria-label="Fechar menu"
              >
                <X className="size-4" />
              </Button>
            </div>

            <div className="flex items-center gap-3 border-b border-sidebar-border px-4 py-4">
              <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary-soft text-xs font-semibold text-primary-soft-foreground">
                {initials}
              </span>
              <div className="min-w-0">
                <p className="truncate text-sm font-medium">{user.name}</p>
                <p className="truncate text-xs text-muted-foreground">{user.email}</p>
              </div>
            </div>

            <nav className="flex flex-1 flex-col gap-1 overflow-y-auto px-2 py-3" aria-label="Principal">
              {primary.map((item) => {
                const Icon = item.icon;
                const active =
                  pathname === item.href || pathname.startsWith(`${item.href}/`);

                return (
                  <Link
                    key={`${item.label}-${item.href}`}
                    href={item.href}
                    onClick={onClose}
                    aria-current={active ? "page" : undefined}
                    className={cn(
                      "relative flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition-colors",
                      active
                        ? "bg-sidebar-accent text-sidebar-accent-foreground"
                        : "text-muted-foreground hover:bg-sidebar-item-hover hover:text-foreground",
                    )}
                  >
                    {active && (
                      <span
                        aria-hidden
                        className="absolute top-2 bottom-2 left-0 w-0.5 rounded-full bg-primary"
                      />
                    )}
                    <Icon className={cn("size-4", active && "text-primary-hover")} />
                    {item.label}
                  </Link>
                );
              })}
            </nav>

            <div className="border-t border-sidebar-border p-3">
              <Button
                variant="ghost"
                className="w-full justify-start gap-2 text-muted-foreground"
                onClick={() => {
                  onClose();
                  onLogout();
                }}
              >
                <LogOut className="size-4" /> Sair
              </Button>
            </div>
          </motion.aside>
        </div>
      )}
    </AnimatePresence>
  );
}
