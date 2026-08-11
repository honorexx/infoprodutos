"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { toast } from "sonner";
import { Search, Shield, UserRound } from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { apiFetch, ApiError } from "@/lib/api-client";
import type { PageResponse, RoleCode, UserSummary } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";

const ASSIGNABLE_ROLES: RoleCode[] = ["SUPER_ADMIN", "INSTRUCTOR", "STUDENT"];

function formatDate(value: string | null) {
  if (!value) return "—";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  }).format(new Date(value));
}

function UsersAdminContent() {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [isLoading, setIsLoading] = useState(true);
  const [pendingId, setPendingId] = useState<string | null>(null);
  const [query, setQuery] = useState("");

  const loadUsers = useCallback(async () => {
    setIsLoading(true);
    try {
      const page = await apiFetch<PageResponse<UserSummary>>("/users?size=100&sort=createdAt,desc");
      setUsers(page.content ?? []);
      setTotal(page.totalElements ?? page.content?.length ?? 0);
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Erro ao carregar usuários.",
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // Carregamento inicial de dados via API; setState assíncrono após o await é intencional.
    void loadUsers();
  }, [loadUsers]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return users;
    return users.filter(
      (u) => u.name.toLowerCase().includes(q) || u.email.toLowerCase().includes(q),
    );
  }, [users, query]);

  const activeCount = users.filter((u) => u.status === "ACTIVE").length;
  const blockedCount = users.filter((u) => u.status === "BLOCKED").length;

  async function toggleBlock(user: UserSummary) {
    setPendingId(user.id);
    const action = user.status === "ACTIVE" ? "block" : "unblock";
    try {
      await apiFetch<void>(`/users/${user.id}/${action}`, { method: "POST" });
      toast.success(action === "block" ? "Usuário bloqueado." : "Usuário desbloqueado.");
      await loadUsers();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha na operação.",
      );
    } finally {
      setPendingId(null);
    }
  }

  async function assignRole(user: UserSummary, roleCode: RoleCode) {
    setPendingId(user.id);
    try {
      await apiFetch<void>(`/users/${user.id}/roles`, { method: "POST", body: { roleCode } });
      toast.success(`Papel ${roleCode} atribuído a ${user.name}.`);
      await loadUsers();
    } catch (error) {
      toast.error(
        error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha na operação.",
      );
    } finally {
      setPendingId(null);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-5 p-4 sm:p-6 lg:p-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <span className="kicker">Administração</span>
          <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight sm:text-3xl">
            Usuários
          </h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Contas, papéis e bloqueios. Ações exigem SUPER_ADMIN.
          </p>
        </div>
        <div className="relative w-full max-w-xs">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Buscar nome ou e-mail…"
            className="h-9 pl-8 text-sm"
          />
        </div>
      </div>

      <div className="grid grid-cols-3 gap-px overflow-hidden rounded-md border border-border bg-border">
        {[
          { label: "Total", value: total, icon: UserRound },
          { label: "Ativos", value: activeCount, icon: UserRound },
          { label: "Bloqueados", value: blockedCount, icon: Shield },
        ].map((stat) => (
          <div key={stat.label} className="flex items-center gap-3 bg-surface px-4 py-3">
            <stat.icon className="size-3.5 text-primary" />
            <div>
              <p className="font-mono text-lg leading-none text-primary">{stat.value}</p>
              <p className="mt-1 text-[0.625rem] font-medium tracking-[0.12em] text-subtle-foreground uppercase">
                {stat.label}
              </p>
            </div>
          </div>
        ))}
      </div>

      <div className="overflow-hidden rounded-md border border-border">
        {isLoading ? (
          <div className="space-y-0 divide-y divide-border">
            <Skeleton className="h-9 w-full rounded-none" />
            <Skeleton className="h-10 w-full rounded-none" />
            <Skeleton className="h-10 w-full rounded-none" />
            <Skeleton className="h-10 w-full rounded-none" />
          </div>
        ) : filtered.length === 0 ? (
          <p className="px-4 py-10 text-center text-sm text-muted-foreground">
            Nenhum usuário encontrado.
          </p>
        ) : (
          <Table>
            <TableHeader>
              <TableRow className="hover:bg-transparent">
                <TableHead className="h-8 px-3">Nome</TableHead>
                <TableHead className="h-8 px-3">E-mail</TableHead>
                <TableHead className="h-8 px-3">Papéis</TableHead>
                <TableHead className="h-8 px-3">Status</TableHead>
                <TableHead className="h-8 px-3">Criado</TableHead>
                <TableHead className="h-8 px-3">Último acesso</TableHead>
                <TableHead className="h-8 px-3 text-right">Ações</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((user) => (
                <TableRow key={user.id} className="h-11">
                  <TableCell className="px-3 py-1.5 font-medium">{user.name}</TableCell>
                  <TableCell className="px-3 py-1.5 text-muted-foreground">{user.email}</TableCell>
                  <TableCell className="px-3 py-1.5">
                    <div className="flex flex-wrap gap-1">
                      {user.roles.map((role) => (
                        <Badge
                          key={role}
                          variant="outline"
                          className="px-1.5 py-0 text-[10px] font-medium tracking-normal normal-case"
                        >
                          {role}
                        </Badge>
                      ))}
                    </div>
                  </TableCell>
                  <TableCell className="px-3 py-1.5">
                    <span
                      className={cn(
                        "inline-flex items-center gap-1.5 text-xs font-medium",
                        user.status === "ACTIVE" ? "text-primary" : "text-danger",
                      )}
                    >
                      <span
                        className={cn(
                          "size-1.5 rounded-full",
                          user.status === "ACTIVE" ? "bg-primary" : "bg-danger",
                        )}
                      />
                      {user.status === "ACTIVE" ? "Ativo" : "Bloqueado"}
                    </span>
                  </TableCell>
                  <TableCell className="px-3 py-1.5 font-mono text-xs text-muted-foreground">
                    {formatDate(user.createdAt)}
                  </TableCell>
                  <TableCell className="px-3 py-1.5 font-mono text-xs text-muted-foreground">
                    {formatDate(user.lastLoginAt)}
                  </TableCell>
                  <TableCell className="px-3 py-1.5">
                    <div className="flex justify-end gap-1.5">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            size="sm"
                            variant="outline"
                            className="h-7 px-2 text-xs"
                            disabled={pendingId === user.id}
                          >
                            Papel
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          {ASSIGNABLE_ROLES.map((role) => (
                            <DropdownMenuItem key={role} onClick={() => assignRole(user, role)}>
                              {role}
                            </DropdownMenuItem>
                          ))}
                        </DropdownMenuContent>
                      </DropdownMenu>
                      <Button
                        size="sm"
                        variant={user.status === "ACTIVE" ? "destructive" : "outline"}
                        className="h-7 px-2 text-xs"
                        disabled={pendingId === user.id}
                        onClick={() => toggleBlock(user)}
                      >
                        {user.status === "ACTIVE" ? "Bloquear" : "Desbloquear"}
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>
    </div>
  );
}

export default function UsersAdminPage() {
  return (
    <ProtectedRoute allowedRoles={["SUPER_ADMIN"]}>
      <DashboardShell>
        <UsersAdminContent />
      </DashboardShell>
    </ProtectedRoute>
  );
}
