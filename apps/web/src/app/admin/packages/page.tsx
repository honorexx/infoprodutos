"use client";

import { useCallback, useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Plus, Trash2 } from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { apiFetch, ApiError } from "@/lib/api-client";
import type { CourseSummary, PageResponse, ProductPackage } from "@/lib/types";
import { packageFormSchema, type PackageFormInput } from "@/lib/validation";
import { formatBrlFromCents } from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

function PackagesContent() {
  const [packages, setPackages] = useState<ProductPackage[] | null>(null);
  const [courses, setCourses] = useState<CourseSummary[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<ProductPackage | null>(null);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    control,
    formState: { errors, isSubmitting },
  } = useForm<PackageFormInput>({
    resolver: zodResolver(packageFormSchema),
    defaultValues: { active: true, courseIds: [], priceReais: "2000" },
  });

  const selectedIds = useWatch({ control, name: "courseIds" }) ?? [];

  const load = useCallback(async () => {
    try {
      const [pkgs, coursePage] = await Promise.all([
        apiFetch<ProductPackage[]>("/admin/packages"),
        apiFetch<PageResponse<CourseSummary>>("/courses?size=100&sort=title,asc"),
      ]);
      setPackages(pkgs);
      setCourses(coursePage.content ?? []);
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Erro ao carregar pacotes.");
      setPackages([]);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  function openCreate() {
    setEditing(null);
    reset({ title: "", description: "", priceReais: "2000", active: true, courseIds: [] });
    setDialogOpen(true);
  }

  function openEdit(pkg: ProductPackage) {
    setEditing(pkg);
    reset({
      title: pkg.title,
      description: pkg.description ?? "",
      priceReais: (pkg.priceCents / 100).toFixed(2),
      active: pkg.active,
      courseIds: pkg.courses.map((c) => c.id),
    });
    setDialogOpen(true);
  }

  function toggleCourse(id: string) {
    const next = selectedIds.includes(id)
      ? selectedIds.filter((x) => x !== id)
      : [...selectedIds, id];
    setValue("courseIds", next, { shouldValidate: true });
  }

  async function onSubmit(data: PackageFormInput) {
    const body = {
      title: data.title,
      description: data.description || null,
      priceCents: Math.round(Number(data.priceReais) * 100),
      active: data.active,
      courseIds: data.courseIds,
    };
    try {
      if (editing) {
        await apiFetch(`/admin/packages/${editing.id}`, { method: "PUT", body });
        toast.success("Pacote atualizado.");
      } else {
        await apiFetch("/admin/packages", { method: "POST", body });
        toast.success("Pacote criado.");
      }
      setDialogOpen(false);
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao salvar pacote.");
    }
  }

  async function onDelete(pkg: ProductPackage) {
    if (!confirm(`Excluir o pacote "${pkg.title}"?`)) return;
    try {
      await apiFetch(`/admin/packages/${pkg.id}`, { method: "DELETE" });
      toast.success("Pacote excluído.");
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao excluir.");
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-5 p-4 sm:p-6 lg:p-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <span className="kicker">Comércio</span>
          <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight sm:text-3xl">Pacotes</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Um preço, vários cursos liberados após o pagamento.
          </p>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button size="sm" className="gap-1.5" onClick={openCreate}>
              <Plus className="size-4" />
              Novo pacote
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-lg">
            <DialogHeader>
              <DialogTitle>{editing ? "Editar pacote" : "Novo pacote"}</DialogTitle>
              <DialogDescription>
                Após o pagamento no Mercado Pago, todos os cursos selecionados são liberados.
              </DialogDescription>
            </DialogHeader>
            <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
              <div className="flex flex-col gap-2">
                <Label htmlFor="pkg-title">Título</Label>
                <Input id="pkg-title" {...register("title")} />
                {errors.title && <p className="text-sm text-destructive">{errors.title.message}</p>}
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="pkg-desc">Descrição</Label>
                <Textarea id="pkg-desc" rows={2} {...register("description")} />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="pkg-price">Preço (R$)</Label>
                <Input id="pkg-price" type="number" min={0.01} step="0.01" {...register("priceReais")} />
                {errors.priceReais && (
                  <p className="text-sm text-destructive">{errors.priceReais.message}</p>
                )}
              </div>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" {...register("active")} />
                Pacote ativo na vitrine
              </label>
              <div className="flex flex-col gap-2">
                <Label>Cursos inclusos</Label>
                <ul className="max-h-48 space-y-1.5 overflow-y-auto rounded-md border border-border p-2">
                  {courses.map((c) => (
                    <li key={c.id}>
                      <label className="flex cursor-pointer items-center gap-2 text-sm">
                        <input
                          type="checkbox"
                          checked={selectedIds.includes(c.id)}
                          onChange={() => toggleCourse(c.id)}
                        />
                        <span className="truncate">{c.title}</span>
                        <span className="ml-auto font-mono text-[11px] text-muted-foreground">
                          {formatBrlFromCents(c.priceCents ?? 0)}
                        </span>
                      </label>
                    </li>
                  ))}
                </ul>
                {errors.courseIds && (
                  <p className="text-sm text-destructive">{errors.courseIds.message}</p>
                )}
              </div>
              <DialogFooter>
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? "Salvando…" : "Salvar"}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {packages === null ? (
        <Skeleton className="h-40 w-full" />
      ) : packages.length === 0 ? (
        <p className="rounded-md border border-dashed border-border p-8 text-sm text-muted-foreground">
          Nenhum pacote ainda. Crie um com preço único e vários cursos.
        </p>
      ) : (
        <ul className="flex flex-col divide-y divide-border overflow-hidden rounded-md border border-border">
          {packages.map((pkg) => (
            <li key={pkg.id} className="flex flex-wrap items-center justify-between gap-3 px-4 py-3">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-medium tracking-tight">{pkg.title}</p>
                  <Badge variant={pkg.active ? "gold" : "outline"}>
                    {pkg.active ? "Ativo" : "Inativo"}
                  </Badge>
                </div>
                <p className="mt-0.5 text-sm text-muted-foreground">
                  {formatBrlFromCents(pkg.priceCents)} · {pkg.courses.length} curso
                  {pkg.courses.length === 1 ? "" : "s"}
                </p>
                <p className="mt-1 truncate text-xs text-subtle-foreground">
                  {pkg.courses.map((c) => c.title).join(" · ")}
                </p>
              </div>
              <div className="flex gap-1">
                <Button variant="outline" size="sm" onClick={() => openEdit(pkg)}>
                  Editar
                </Button>
                <Button variant="ghost" size="icon-sm" onClick={() => void onDelete(pkg)}>
                  <Trash2 className="size-3.5" />
                </Button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default function AdminPackagesPage() {
  return (
    <ProtectedRoute allowedRoles={["SUPER_ADMIN"]}>
      <DashboardShell>
        <PackagesContent />
      </DashboardShell>
    </ProtectedRoute>
  );
}
