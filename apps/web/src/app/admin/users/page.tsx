"use client";

import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { apiFetch, ApiError } from "@/lib/api-client";
import type { PageResponse, RoleCode, UserSummary } from "@/lib/types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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

const ASSIGNABLE_ROLES: RoleCode[] = ["SUPER_ADMIN", "INSTRUCTOR", "STUDENT"];

function UsersAdminContent() {
  const [users, setUsers] = useState<UserSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [pendingId, setPendingId] = useState<string | null>(null);

  const loadUsers = useCallback(async () => {
    setIsLoading(true);
    try {
      const page = await apiFetch<PageResponse<UserSummary>>("/users?size=50&sort=createdAt,desc");
      setUsers(page.content);
    } catch (error) {
      toast.error(error instanceof ApiError ? error.body?.detail ?? error.message : "Erro ao carregar usuários.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // Carregamento inicial de dados via API; setState assíncrono após o await é intencional.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void loadUsers();
  }, [loadUsers]);

  async function toggleBlock(user: UserSummary) {
    setPendingId(user.id);
    const action = user.status === "ACTIVE" ? "block" : "unblock";
    try {
      await apiFetch<void>(`/users/${user.id}/${action}`, { method: "POST" });
      toast.success(action === "block" ? "Usuário bloqueado." : "Usuário desbloqueado.");
      await loadUsers();
    } catch (error) {
      toast.error(error instanceof ApiError ? error.body?.detail ?? error.message : "Falha na operação.");
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
      toast.error(error instanceof ApiError ? error.body?.detail ?? error.message : "Falha na operação.");
    } finally {
      setPendingId(null);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-6 p-6 sm:p-8">
      <div>
        <span className="kicker">Administração</span>
        <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight sm:text-3xl">Usuários</h1>
        <p className="mt-1 text-muted-foreground">
          Gerencie contas, bloqueios e papéis da plataforma. Ações administrativas exigem papel
          SUPER_ADMIN.
        </p>
      </div>

      <div className="rounded-lg border border-border/70">
          {isLoading ? (
            <div className="space-y-2 p-4">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Nome</TableHead>
                  <TableHead>E-mail</TableHead>
                  <TableHead>Papéis</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead className="text-right">Ações</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell className="font-medium">{user.name}</TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell className="space-x-1">
                      {user.roles.map((role) => (
                        <Badge key={role} variant="secondary">
                          {role}
                        </Badge>
                      ))}
                    </TableCell>
                    <TableCell>
                      <Badge variant={user.status === "ACTIVE" ? "success" : "destructive"}>
                        {user.status === "ACTIVE" ? "Ativo" : "Bloqueado"}
                      </Badge>
                    </TableCell>
                    <TableCell className="flex justify-end gap-2">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button size="sm" variant="outline" disabled={pendingId === user.id}>
                            Atribuir papel
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent>
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
                        disabled={pendingId === user.id}
                        onClick={() => toggleBlock(user)}
                      >
                        {user.status === "ACTIVE" ? "Bloquear" : "Desbloquear"}
                      </Button>
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
