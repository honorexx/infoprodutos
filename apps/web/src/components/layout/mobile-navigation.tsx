"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { AnimatePresence, motion } from "framer-motion";
import { X, LogOut } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { cn } from "@/lib/utils";
import { primaryNavigation, upcomingNavigation, filterNavByRole } from "@/config/navigation";
import { LogoMark } from "@/components/logo";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

/** Navegação em drawer para telas pequenas, com os mesmos itens da sidebar de desktop. */
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
  const upcoming = filterNavByRole(upcomingNavigation, hasRole);

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-50 flex md:hidden">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.18 }}
            className="absolute inset-0 bg-navy-950/70"
            onClick={onClose}
          />
          <motion.aside
            initial={{ x: -280 }}
            animate={{ x: 0 }}
            exit={{ x: -280 }}
            transition={{ duration: 0.22, ease: [0.22, 1, 0.36, 1] }}
            className="relative flex w-[280px] flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground"
          >
            <div className="flex items-center justify-between gap-2 border-b border-sidebar-border/60 px-3 py-3.5">
              <LogoMark variant="ink" className="size-11" />
              <Button
                variant="ghost"
                size="icon-sm"
                onClick={onClose}
                aria-label="Fechar menu"
                className="text-sidebar-foreground hover:bg-primary-hover hover:text-sidebar-foreground"
              >
                <X className="size-4" />
              </Button>
            </div>

            <nav className="flex flex-1 flex-col gap-4 overflow-y-auto px-3 py-2">
              <div className="flex flex-col gap-0.5">
                {primary.map((item) => {
                  const Icon = item.icon;
                  const active = pathname === item.href || pathname.startsWith(item.href + "/");
                  return (
                    <Link
                      key={item.href}
                      href={item.href}
                      onClick={onClose}
                      aria-current={active ? "page" : undefined}
                      className={cn(
                        "flex items-center gap-3 rounded-md border-l-2 px-2.5 py-2.5 text-sm font-medium transition-colors",
                        active
                          ? "border-navy-950 bg-sidebar-accent text-sidebar-foreground"
                          : "border-transparent text-sidebar-muted hover:bg-primary-hover hover:text-sidebar-foreground",
                      )}
                    >
                      <Icon className={cn("size-4 shrink-0", active && "text-navy-950")} />
                      {item.label}
                    </Link>
                  );
                })}
              </div>

              {upcoming.length > 0 && (
                <div className="flex flex-col gap-0.5">
                  <p className="px-2.5 pb-1 text-[10px] font-semibold tracking-[0.14em] text-sidebar-muted/80 uppercase">
                    Próximas fases
                  </p>
                  {upcoming.map((item) => {
                    const Icon = item.icon;
                    return (
                      <span
                        key={item.label}
                        aria-disabled="true"
                        className="flex items-center justify-between gap-2 rounded-md px-2.5 py-2.5 text-sm font-medium text-sidebar-muted/70"
                      >
                        <span className="flex items-center gap-3">
                          <Icon className="size-4 shrink-0" />
                          {item.label}
                        </span>
                        <Badge
                          variant="secondary"
                          className="border-sidebar-border bg-navy-950/10 text-sidebar-muted"
                        >
                          Em breve
                        </Badge>
                      </span>
                    );
                  })}
                </div>
              )}
            </nav>

            <div className="border-t border-sidebar-border p-3">
              <Button
                variant="ghost"
                size="sm"
                onClick={onLogout}
                className="w-full justify-start gap-2 text-sidebar-muted hover:bg-primary-hover hover:text-sidebar-foreground"
              >
                <LogOut className="size-4" />
                Sair
              </Button>
            </div>
          </motion.aside>
        </div>
      )}
    </AnimatePresence>
  );
}
