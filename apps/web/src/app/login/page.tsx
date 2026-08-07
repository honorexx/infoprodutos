"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { ApiError } from "@/lib/api-client";
import { loginSchema, type LoginInput } from "@/lib/validation";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { AuthSplitLayout } from "@/components/auth-split-layout";
import { Skeleton } from "@/components/ui/skeleton";

export default function LoginPage() {
  const { login, user, isLoading } = useAuth();
  const router = useRouter();
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({ resolver: zodResolver(loginSchema) });

  useEffect(() => {
    if (!isLoading && user) {
      router.replace("/dashboard");
    }
  }, [isLoading, user, router]);

  async function onSubmit(data: LoginInput) {
    setServerError(null);
    try {
      await login(data.email, data.password);
      router.push("/dashboard");
    } catch (error) {
      if (error instanceof ApiError) {
        setServerError(error.body?.detail ?? "Não foi possível entrar. Tente novamente.");
      } else {
        setServerError("Não foi possível entrar. Tente novamente.");
      }
    }
  }

  if (isLoading || user) {
    return (
      <div className="flex flex-1 items-center justify-center p-8">
        <Skeleton className="h-40 w-full max-w-sm" />
      </div>
    );
  }

  return (
    <AuthSplitLayout
      kicker="Bem-vindo de volta"
      title="Entre para continuar de onde parou."
      description="Acesse o painel administrativo, sua área de professor ou sua área de aluno."
    >
      <div className="flex flex-col gap-1.5">
        <h1 className="font-heading text-2xl font-medium tracking-tight">Entrar</h1>
        <p className="text-sm text-muted-foreground">Acesse sua conta na plataforma.</p>
      </div>
      <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
        {serverError && (
          <Alert variant="destructive">
            <AlertDescription>{serverError}</AlertDescription>
          </Alert>
        )}
        <div className="flex flex-col gap-2">
          <Label htmlFor="email">E-mail</Label>
          <Input id="email" type="email" autoComplete="email" {...register("email")} />
          {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="password">Senha</Label>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            {...register("password")}
          />
          {errors.password && (
            <p className="text-sm text-destructive">{errors.password.message}</p>
          )}
        </div>
        <Button type="submit" disabled={isSubmitting} className="mt-2 gap-1.5">
          {isSubmitting ? "Entrando..." : "Entrar"}
          {!isSubmitting && <ArrowRight className="size-4" />}
        </Button>
      </form>
      <p className="text-center text-sm text-muted-foreground">
        Ainda não tem conta?{" "}
        <Link href="/register" className="font-medium text-accent underline-offset-4 hover:underline">
          Cadastre-se
        </Link>
      </p>
    </AuthSplitLayout>
  );
}
