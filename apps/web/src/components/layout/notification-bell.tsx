"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { Bell } from "lucide-react";
import { apiFetch } from "@/lib/api-client";
import type { NotificationListResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

function formatRelative(iso: string) {
  try {
    const date = new Date(iso);
    const diffMs = Date.now() - date.getTime();
    const mins = Math.floor(diffMs / 60_000);
    if (mins < 1) return "agora";
    if (mins < 60) return `há ${mins} min`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `há ${hours} h`;
    const days = Math.floor(hours / 24);
    return `há ${days} d`;
  } catch {
    return "";
  }
}

export function NotificationBell() {
  const [data, setData] = useState<NotificationListResponse | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await apiFetch<NotificationListResponse>("/notifications?size=20");
      setData(response);
    } catch {
      setData({ unreadCount: 0, items: [] });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
    const id = window.setInterval(() => void load(), 60_000);
    return () => window.clearInterval(id);
  }, [load]);

  async function markOne(id: string) {
    try {
      await apiFetch<void>(`/notifications/${id}/read`, { method: "POST" });
      await load();
    } catch {
      // silencioso — o usuário ainda pode navegar pelo link
    }
  }

  async function markAll() {
    try {
      await apiFetch<void>("/notifications/read-all", { method: "POST" });
      await load();
    } catch {
      // ignore
    }
  }

  const unread = data?.unreadCount ?? 0;
  const items = data?.items ?? [];

  return (
    <DropdownMenu
      onOpenChange={(open) => {
        if (open) void load();
      }}
    >
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" aria-label="Notificações" className="relative">
          <Bell className="size-4.5" />
          {unread > 0 && (
            <span className="absolute top-1.5 right-1.5 size-1.5 rounded-full bg-accent" />
          )}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-80">
        <div className="flex items-center justify-between gap-2 px-2 py-1.5">
          <DropdownMenuLabel className="p-0">Notificações</DropdownMenuLabel>
          {unread > 0 && (
            <button
              type="button"
              onClick={() => void markAll()}
              className="text-[11px] font-medium text-accent hover:underline"
            >
              Marcar todas como lidas
            </button>
          )}
        </div>
        <DropdownMenuSeparator />
        {loading && items.length === 0 && (
          <p className="px-3 py-3 text-sm text-muted-foreground">Carregando…</p>
        )}
        {!loading && items.length === 0 && (
          <p className="px-3 py-3 text-sm text-muted-foreground">Nenhuma notificação por enquanto.</p>
        )}
        {items.map((item) => {
          const content = (
            <div className="flex flex-col gap-0.5 whitespace-normal">
              <span className={`text-sm ${item.read ? "font-normal" : "font-medium"}`}>{item.title}</span>
              <span className="text-xs leading-relaxed text-muted-foreground">{item.body}</span>
              <span className="text-[10px] text-muted-foreground/80">{formatRelative(item.createdAt)}</span>
            </div>
          );
          if (item.linkHref) {
            return (
              <DropdownMenuItem
                key={item.id}
                className={`items-start ${item.read ? "" : "bg-primary-soft/40"}`}
                asChild
                onSelect={() => {
                  void markOne(item.id);
                }}
              >
                <Link href={item.linkHref} className="w-full cursor-pointer">
                  {content}
                </Link>
              </DropdownMenuItem>
            );
          }
          return (
            <DropdownMenuItem
              key={item.id}
              className={`items-start ${item.read ? "" : "bg-primary-soft/40"}`}
              onSelect={() => {
                void markOne(item.id);
              }}
            >
              {content}
            </DropdownMenuItem>
          );
        })}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
