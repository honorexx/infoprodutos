"use client";

import { useState } from "react";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { useAuth } from "@/lib/auth-context";
import { apiFetch, ApiError } from "@/lib/api-client";
import { changePasswordSchema, type ChangePasswordInput } from "@/lib/validation";
import type { RoleCode } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Alert, AlertDescription } from "@/components/ui/alert";

const ROLE_LABELS: Record<RoleCode, string> = {
  SUPER_ADMIN: "Administrador",
  INSTRUCTOR: "Professor",
  STUDENT: "Aluno",
};

function SettingsContent() {
  const { user } = useAuth();
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ChangePasswordInput>({ resolver: zodResolver(changePasswordSchema) });

  if (!user) return null;

  async function onChangePassword(data: ChangePasswordInput) {
    setServerError(null);
    try {
      await apiFetch<void>("/auth/password/change", {
        method: "POST",
        body: {
          currentPassword: data.currentPassword,
          newPassword: data.newPassword,
        },
      });
      reset();
      toast.success("Senha atualizada.");
    } catch (error) {
      if (error instanceof ApiError) {
        setServerError(error.body?.detail ?? "Não foi possível alterar a senha.");
      } else {
        setServerError("Não foi possível alterar a senha. Tente novamente.");
      }
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-10 p-6 sm:p-8">
      <div>
        <span className="kicker">Conta</span>
        <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight sm:text-3xl">
          Configurações
        </h1>
        <p className="mt-1 text-muted-foreground">
          Dados da sua conta e alteração de senha.
        </p>
      </div>

      <section className="flex flex-col gap-5 border-t border-border/70 pt-6">
        <h2 className="font-heading text-lg font-medium tracking-tight">Perfil</h2>
        <dl className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1">
            <dt className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
              Nome
            </dt>
            <dd className="text-sm text-foreground">{user.name}</dd>
          </div>
          <div className="flex flex-col gap-1">
            <dt className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
              E-mail
            </dt>
            <dd className="text-sm text-foreground">{user.email}</dd>
          </div>
          <div className="flex flex-col gap-1 sm:col-span-2">
            <dt className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
              Papéis
            </dt>
            <dd className="flex flex-wrap gap-1.5">
              {user.roles.map((role) => (
                <Badge key={role} variant="secondary">
                  {ROLE_LABELS[role] ?? role}
                </Badge>
              ))}
            </dd>
          </div>
        </dl>
        <p className="text-sm text-muted-foreground">
          Para alterar nome ou e-mail, fale com a administração.{" "}
          <Link href="/dashboard" className="text-accent underline-offset-4 hover:underline">
            Voltar ao painel
          </Link>
        </p>
      </section>

      <section className="flex flex-col gap-5 border-t border-border/70 pt-6">
        <div>
          <h2 className="font-heading text-lg font-medium tracking-tight">Senha</h2>
          <p className="mt-1 text-sm text-muted-foreground">
            Use uma senha com pelo menos 8 caracteres.
          </p>
        </div>
        <form onSubmit={handleSubmit(onChangePassword)} className="flex max-w-md flex-col gap-4">
          {serverError && (
            <Alert variant="destructive">
              <AlertDescription>{serverError}</AlertDescription>
            </Alert>
          )}
          <div className="flex flex-col gap-2">
            <Label htmlFor="currentPassword">Senha atual</Label>
            <Input
              id="currentPassword"
              type="password"
              autoComplete="current-password"
              {...register("currentPassword")}
            />
            {errors.currentPassword && (
              <p className="text-sm text-destructive">{errors.currentPassword.message}</p>
            )}
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="newPassword">Nova senha</Label>
            <Input
              id="newPassword"
              type="password"
              autoComplete="new-password"
              {...register("newPassword")}
            />
            {errors.newPassword && (
              <p className="text-sm text-destructive">{errors.newPassword.message}</p>
            )}
          </div>
          <div className="flex flex-col gap-2">
            <Label htmlFor="confirmNewPassword">Confirmar nova senha</Label>
            <Input
              id="confirmNewPassword"
              type="password"
              autoComplete="new-password"
              {...register("confirmNewPassword")}
            />
            {errors.confirmNewPassword && (
              <p className="text-sm text-destructive">{errors.confirmNewPassword.message}</p>
            )}
          </div>
          <Button type="submit" disabled={isSubmitting} className="mt-1 w-fit">
            {isSubmitting ? "Salvando..." : "Atualizar senha"}
          </Button>
        </form>
      </section>
    </div>
  );
}

export default function SettingsPage() {
  return (
    <ProtectedRoute>
      <DashboardShell>
        <SettingsContent />
      </DashboardShell>
    </ProtectedRoute>
  );
}
