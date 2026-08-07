"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { ArrowRight, Plus, Search } from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { StatusBadge } from "@/components/status-badge";
import { apiFetch, ApiError } from "@/lib/api-client";
import { courseFormSchema, type CourseFormInput } from "@/lib/validation";
import type { CourseSummary, PageResponse } from "@/lib/types";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

function CoursesContent() {
  const router = useRouter();
  const [courses, setCourses] = useState<CourseSummary[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [query, setQuery] = useState("");

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CourseFormInput>({ resolver: zodResolver(courseFormSchema) });

  const loadCourses = useCallback(async () => {
    setIsLoading(true);
    try {
      const page = await apiFetch<PageResponse<CourseSummary>>("/courses?size=50&sort=updatedAt,desc");
      setCourses(page.content);
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Erro ao carregar cursos.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void loadCourses();
  }, [loadCourses]);

  async function onCreate(data: CourseFormInput) {
    try {
      const course = await apiFetch<{ id: string }>("/courses", {
        method: "POST",
        body: {
          title: data.title,
          description: data.description || null,
          workloadHours: Number(data.workloadHours),
        },
      });
      toast.success("Curso criado com sucesso.");
      setDialogOpen(false);
      reset();
      router.push(`/courses/${course.id}`);
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao criar curso.");
    }
  }

  const filtered = courses.filter((c) => c.title.toLowerCase().includes(query.toLowerCase()));

  return (
    <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-6 p-6 sm:p-8">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <span className="kicker">Painel do professor</span>
          <h1 className="mt-2 font-heading text-2xl font-medium tracking-tight sm:text-3xl">Cursos</h1>
          <p className="mt-1 text-muted-foreground">Gerencie a estrutura curricular dos seus cursos.</p>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button className="gap-1.5">
              <Plus className="size-4" />
              Novo curso
            </Button>
          </DialogTrigger>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>Criar novo curso</DialogTitle>
              <DialogDescription>O curso é criado como rascunho e pode ser publicado depois.</DialogDescription>
            </DialogHeader>
            <form onSubmit={handleSubmit(onCreate)} className="flex flex-col gap-4">
              <div className="flex flex-col gap-2">
                <Label htmlFor="title">Título</Label>
                <Input id="title" {...register("title")} />
                {errors.title && <p className="text-sm text-destructive">{errors.title.message}</p>}
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="description">Descrição</Label>
                <Textarea id="description" rows={3} {...register("description")} />
              </div>
              <div className="flex flex-col gap-2">
                <Label htmlFor="workloadHours">Carga horária (obrigatória)</Label>
                <Input id="workloadHours" type="number" min={0.5} step="0.5" required {...register("workloadHours")} />
                {errors.workloadHours && (
                  <p className="text-sm text-destructive">{errors.workloadHours.message}</p>
                )}
              </div>
              <DialogFooter>
                <Button type="submit" disabled={isSubmitting}>
                  {isSubmitting ? "Criando..." : "Criar curso"}
                </Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      {isLoading ? (
        <div className="flex flex-col divide-y divide-border/70 rounded-lg border border-border/70">
          <Skeleton className="h-16 w-full rounded-none" />
          <Skeleton className="h-16 w-full rounded-none" />
          <Skeleton className="h-16 w-full rounded-none" />
        </div>
      ) : courses.length === 0 ? (
        <div className="flex flex-col items-center gap-2 rounded-lg border border-dashed border-border/70 py-16 text-center text-muted-foreground">
          <p>Você ainda não tem nenhum curso.</p>
          <p className="text-sm">Clique em &quot;Novo curso&quot; para começar.</p>
        </div>
      ) : (
        <div className="flex flex-col gap-3">
          {courses.length > 5 && (
            <div className="relative max-w-xs">
              <Search className="pointer-events-none absolute top-1/2 left-2.5 size-3.5 -translate-y-1/2 text-muted-foreground" />
              <Input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Buscar curso..."
                className="pl-8"
              />
            </div>
          )}
          <ul className="flex flex-col divide-y divide-border/70 rounded-lg border border-border/70">
            {filtered.map((course) => (
              <li key={course.id}>
                <Link
                  href={`/courses/${course.id}`}
                  className="group/row flex items-center justify-between gap-4 px-5 py-4 transition-colors hover:bg-muted/40"
                >
                  <div className="flex min-w-0 flex-1 items-center gap-4">
                    <div className="min-w-0 flex-1">
                      <p className="truncate font-heading text-base font-medium tracking-tight">
                        {course.title}
                      </p>
                      <p className="mt-0.5 truncate text-sm text-muted-foreground">
                        {course.workloadHours ? `${course.workloadHours}h · ` : ""}
                        por {course.createdByName}
                      </p>
                    </div>
                  </div>
                  <div className="flex shrink-0 items-center gap-3">
                    <StatusBadge status={course.status} />
                    <ArrowRight className="size-4 text-muted-foreground/50 transition-transform group-hover/row:translate-x-0.5 group-hover/row:text-foreground" />
                  </div>
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

export default function CoursesPage() {
  return (
    <ProtectedRoute allowedRoles={["SUPER_ADMIN", "INSTRUCTOR"]}>
      <DashboardShell>
        <CoursesContent />
      </DashboardShell>
    </ProtectedRoute>
  );
}
