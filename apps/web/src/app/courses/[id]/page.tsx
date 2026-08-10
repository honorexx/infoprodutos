"use client";

import { use, useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import {
  ArrowLeft,
  ArrowDown,
  ArrowUp,
  Archive,
  ChevronDown,
  Eye,
  EyeOff,
  Info,
  MoreHorizontal,
  Pencil,
  Plus,
  Cpu,
  ImagePlus,
  Trash2,
  Video,
} from "lucide-react";
import { ProtectedRoute } from "@/components/protected-route";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { StatusBadge } from "@/components/status-badge";
import { useAuth } from "@/lib/auth-context";
import {
  apiFetch,
  apiUpload,
  putPresigned,
  ApiError,
  type UploadInitResponse,
} from "@/lib/api-client";
import { ApiImage } from "@/components/ui/api-image";
import { courseFormSchema, lessonFormSchema, moduleFormSchema } from "@/lib/validation";
import type { CourseFormInput, LessonFormInput, ModuleFormInput } from "@/lib/validation";
import type { AiJob, Course, CourseModule, Lesson } from "@/lib/types";
import { CourseEnrollmentsPanel } from "@/components/courses/course-enrollments-panel";
import { ModuleQuizPanel } from "@/components/courses/module-quiz-panel";
import {
  VideoUploadDialog,
  type VideoUploadPayload,
} from "@/components/courses/video-upload-dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";

function formatDuration(seconds: number | null) {
  if (!seconds) return null;
  const minutes = Math.round(seconds / 60);
  return `${minutes} min`;
}

function CourseDetailContent({ courseId }: { courseId: string }) {
  const router = useRouter();
  const { user, hasRole } = useAuth();
  const [course, setCourse] = useState<Course | null>(null);
  const [modules, setModules] = useState<CourseModule[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [editOpen, setEditOpen] = useState(false);
  const [moduleDialogOpen, setModuleDialogOpen] = useState(false);
  const [lessonDialogModuleId, setLessonDialogModuleId] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [collapsedModules, setCollapsedModules] = useState<Record<string, boolean>>({});
  const [uploadingLessonId, setUploadingLessonId] = useState<string | null>(null);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const [videoUploadLesson, setVideoUploadLesson] = useState<Lesson | null>(null);
  const [uploadingCover, setUploadingCover] = useState(false);
  const [thumbnailLessonId, setThumbnailLessonId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    try {
      const [courseData, moduleData] = await Promise.all([
        apiFetch<Course>(`/courses/${courseId}`),
        apiFetch<CourseModule[]>(`/courses/${courseId}/modules`),
      ]);
      setCourse(courseData);
      setModules(moduleData);
    } catch (error) {
      if (error instanceof ApiError && (error.status === 403 || error.status === 404)) {
        toast.error("Curso não encontrado ou sem permissão de acesso.");
        router.replace("/courses");
        return;
      }
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Erro ao carregar curso.");
    } finally {
      setIsLoading(false);
    }
  }, [courseId, router]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  const editForm = useForm<CourseFormInput>({ resolver: zodResolver(courseFormSchema) });
  const moduleForm = useForm<ModuleFormInput>({ resolver: zodResolver(moduleFormSchema) });
  const lessonForm = useForm<LessonFormInput>({
    resolver: zodResolver(lessonFormSchema),
    defaultValues: { accessType: "ENROLLED_ONLY" },
  });

  const canManage =
    hasRole("SUPER_ADMIN") || (course?.instructors.some((i) => i.userId === user?.id) ?? false);

  function toggleModule(moduleId: string) {
    setCollapsedModules((prev) => ({ ...prev, [moduleId]: !prev[moduleId] }));
  }

  async function handleStatusAction(action: "publish" | "unpublish" | "archive") {
    if (!course) return;
    setPending(true);
    try {
      await apiFetch<void>(`/courses/${course.id}/${action}`, { method: "POST" });
      const labels = { publish: "publicado", unpublish: "despublicado", archive: "arquivado" };
      toast.success(`Curso ${labels[action]}.`);
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha na operação.");
    } finally {
      setPending(false);
    }
  }

  function openEditDialog() {
    if (!course) return;
    editForm.reset({
      title: course.title,
      description: course.description ?? "",
      workloadHours: course.workloadHours != null ? String(course.workloadHours) : "",
      priceReais: ((course.priceCents ?? 0) / 100).toFixed(2),
    });
    setEditOpen(true);
  }

  async function onEditCourse(data: CourseFormInput) {
    if (!course) return;
    try {
      await apiFetch<Course>(`/courses/${course.id}`, {
        method: "PUT",
        body: {
          title: data.title,
          description: data.description || null,
          coverImageUrl: course.coverImageUrl,
          workloadHours: Number(data.workloadHours),
          priceCents: Math.round(Number(data.priceReais) * 100),
          minCompletionPercentage: course.minCompletionPercentage,
          minPassingScore: course.minPassingScore,
          certificateEnabled: course.certificateEnabled,
          maxQuizAttempts: course.maxQuizAttempts,
        },
      });
      toast.success("Curso atualizado.");
      setEditOpen(false);
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao atualizar curso.");
    }
  }

  async function onCreateModule(data: ModuleFormInput) {
    try {
      await apiFetch<CourseModule>(`/courses/${courseId}/modules`, {
        method: "POST",
        body: { title: data.title, description: data.description || null },
      });
      toast.success("Módulo criado.");
      setModuleDialogOpen(false);
      moduleForm.reset();
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao criar módulo.");
    }
  }

  async function moveModule(module: CourseModule, direction: -1 | 1) {
    const index = modules.findIndex((m) => m.id === module.id);
    const swapIndex = index + direction;
    if (swapIndex < 0 || swapIndex >= modules.length) return;
    const reordered = [...modules];
    [reordered[index], reordered[swapIndex]] = [reordered[swapIndex], reordered[index]];
    try {
      await apiFetch<void>(`/courses/${courseId}/modules/reorder`, {
        method: "POST",
        body: { orderedIds: reordered.map((m) => m.id) },
      });
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao reordenar.");
    }
  }

  async function deleteModule(module: CourseModule) {
    if (!confirm(`Excluir o módulo "${module.title}"? Esta ação não pode ser desfeita.`)) return;
    try {
      await apiFetch<void>(`/modules/${module.id}`, { method: "DELETE" });
      toast.success("Módulo excluído.");
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao excluir módulo.");
    }
  }

  async function publishModule(module: CourseModule) {
    try {
      await apiFetch<void>(`/modules/${module.id}/publish`, { method: "POST" });
      toast.success("Módulo publicado.");
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao publicar módulo.");
    }
  }

  async function onCreateLesson(moduleId: string, data: LessonFormInput) {
    try {
      await apiFetch<Lesson>(`/modules/${moduleId}/lessons`, {
        method: "POST",
        body: {
          title: data.title,
          description: data.description || null,
          durationSeconds: data.durationSeconds ? Number(data.durationSeconds) : null,
          accessType: data.accessType,
        },
      });
      toast.success("Aula criada.");
      setLessonDialogModuleId(null);
      lessonForm.reset({ accessType: "ENROLLED_ONLY" });
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao criar aula.");
    }
  }

  async function moveLesson(module: CourseModule, lesson: Lesson, direction: -1 | 1) {
    const index = module.lessons.findIndex((l) => l.id === lesson.id);
    const swapIndex = index + direction;
    if (swapIndex < 0 || swapIndex >= module.lessons.length) return;
    const reordered = [...module.lessons];
    [reordered[index], reordered[swapIndex]] = [reordered[swapIndex], reordered[index]];
    try {
      await apiFetch<void>(`/modules/${module.id}/lessons/reorder`, {
        method: "POST",
        body: { orderedIds: reordered.map((l) => l.id) },
      });
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao reordenar.");
    }
  }

  async function deleteLesson(lesson: Lesson) {
    if (!confirm(`Excluir a aula "${lesson.title}"? Esta ação não pode ser desfeita.`)) return;
    try {
      await apiFetch<void>(`/lessons/${lesson.id}`, { method: "DELETE" });
      toast.success("Aula excluída.");
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao excluir aula.");
    }
  }

  async function publishLesson(lesson: Lesson) {
    try {
      await apiFetch<void>(`/lessons/${lesson.id}/publish`, { method: "POST" });
      toast.success("Aula publicada.");
      await load();
    } catch (error) {
      toast.error(error instanceof ApiError ? (error.body?.detail ?? error.message) : "Falha ao publicar aula.");
    }
  }

  async function uploadCourseCover(file: File) {
    if (!course) return;
    setUploadingCover(true);
    try {
      // 1) Tenta DIRECT (R2). 2) Se a rede bloquear Cloudflare, PROXY multipart → Render.
      let usedProxy = false;
      try {
        const init = await apiFetch<{
          uploadMode: string;
          uploadUrl: string | null;
          storageKey: string | null;
          contentType: string | null;
        }>(`/courses/${course.id}/cover-upload-init`, {
          method: "POST",
          body: {
            contentType: file.type || "image/jpeg",
            filename: file.name,
            sizeBytes: file.size,
          },
        });
        if (init.uploadMode === "DIRECT" && init.uploadUrl && init.storageKey) {
          const ct = init.contentType || file.type || "image/jpeg";
          try {
            await putPresigned(init.uploadUrl, file, ct);
            await apiFetch(`/courses/${course.id}/cover-upload-complete`, {
              method: "POST",
              body: { storageKey: init.storageKey, contentType: ct },
            });
          } catch {
            usedProxy = true;
            const form = new FormData();
            form.append("file", file);
            await apiUpload<Course>(`/courses/${course.id}/cover`, form);
          }
        } else {
          usedProxy = true;
          const form = new FormData();
          form.append("file", file);
          const uploadPath = (init.uploadUrl || `/courses/${course.id}/cover`).replace(
            /^\/api\/v1/,
            "",
          );
          await apiUpload<Course>(uploadPath, form);
        }
      } catch {
        usedProxy = true;
        const form = new FormData();
        form.append("file", file);
        await apiUpload<Course>(`/courses/${course.id}/cover`, form);
      }
      toast.success(
        usedProxy ? "Capa do curso atualizada (via API)." : "Capa do curso atualizada.",
      );
      await load();
    } catch (err) {
      toast.error(
        err instanceof ApiError ? (err.body?.detail ?? err.message) : "Falha ao enviar a capa.",
      );
    } finally {
      setUploadingCover(false);
    }
  }

  async function uploadLessonVideo(lesson: Lesson, payload: VideoUploadPayload) {
    setUploadingLessonId(lesson.id);
    setUploadProgress(0);
    try {
      const init = await apiFetch<UploadInitResponse>("/videos/upload-init", {
        method: "POST",
        body: {
          lessonId: lesson.id,
          videoContentType: payload.video.type || "video/mp4",
          videoFilename: payload.video.name,
          videoSizeBytes: payload.video.size,
          thumbnailContentType: payload.thumbnail.type || "image/jpeg",
          thumbnailFilename: payload.thumbnail.name,
          thumbnailSizeBytes: payload.thumbnail.size,
        },
      });

      const runProxyUpload = async () => {
        const form = new FormData();
        form.append("file", payload.video);
        form.append("thumbnail", payload.thumbnail);
        const uploadPath = `/videos/${init.videoAssetId}/upload`;
        // PROXY: Render (API_UPLOAD_BASE_URL); fallback same-origin se CF bloquear.
        await apiUpload(uploadPath, form, "POST", { onProgress: setUploadProgress });
      };

      if (init.uploadMode === "DIRECT") {
        if (!init.videoUploadUrl || !init.thumbnailUploadUrl) {
          throw new Error("API não retornou URLs assinadas de upload.");
        }
        const videoCt = init.videoContentType || payload.video.type || "video/mp4";
        const thumbCt = init.thumbnailContentType || payload.thumbnail.type || "image/jpeg";
        try {
          setUploadProgress(1);
          await putPresigned(init.thumbnailUploadUrl, payload.thumbnail, thumbCt);
          await putPresigned(init.videoUploadUrl, payload.video, videoCt, setUploadProgress);
        } catch {
          // Rede sem Cloudflare/R2 → multipart na API (Render grava no R2).
          toast.message("Upload direto indisponível nesta rede. Enviando via API…");
          await runProxyUpload();
        }
      } else {
        if (!init.uploadUrl) {
          throw new Error("API não retornou URL de upload.");
        }
        await runProxyUpload();
      }

      await apiFetch(`/videos/${init.videoAssetId}/upload-complete`, { method: "POST" });
      toast.success("Vídeo e thumbnail associados à aula");
      setVideoUploadLesson(null);
      await load();
    } catch (err) {
      const detail =
        err instanceof ApiError
          ? (err.body?.detail ?? err.message)
          : "Falha no upload do vídeo.";
      const hint =
        payload.video.size > 80 * 1024 * 1024
          ? " Vídeos longos pedem rede com acesso ao Cloudflare (ex.: hotspot 4G)."
          : "";
      toast.error(`${detail}${hint}`);
    } finally {
      setUploadingLessonId(null);
      setUploadProgress(null);
    }
  }

  async function uploadLessonThumbnail(lesson: Lesson, file: File) {
    if (!lesson.currentVideoAssetId) {
      toast.error("Envie o vídeo da aula antes da thumbnail.");
      return;
    }
    setThumbnailLessonId(lesson.id);
    try {
      const form = new FormData();
      form.append("file", file);
      await apiUpload(`/videos/${lesson.currentVideoAssetId}/thumbnail`, form);
      toast.success("Thumbnail da aula atualizada. Ela aparece como capa do player.");
      await load();
    } catch (err) {
      toast.error(
        err instanceof ApiError
          ? (err.body?.detail ?? err.message)
          : "Falha ao enviar a thumbnail.",
      );
    } finally {
      setThumbnailLessonId(null);
    }
  }

  async function requestAiGeneration(lesson: Lesson) {
    setPending(true);
    try {
      const job = await apiFetch<AiJob>(`/lessons/${lesson.id}/ai-jobs`, {
        method: "POST",
        body: {
          idempotencyKey: crypto.randomUUID(),
          questionCount: 5,
          language: "pt-BR",
        },
      });
      toast.success("Processamento de IA iniciado");
      router.push(`/ai/${job.id}`);
    } catch (err) {
      toast.error(err instanceof ApiError ? err.message : "Não foi possível iniciar a IA.");
    } finally {
      setPending(false);
    }
  }

  if (isLoading || !course) {
    return (
      <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-4 p-4 sm:p-6 lg:p-8">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-24 w-full" />
        <Skeleton className="h-40 w-full" />
      </div>
    );
  }

  const totalLessons = modules.reduce((acc, m) => acc + m.lessons.length, 0);

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-1 flex-col gap-6 p-4 sm:p-6 lg:p-8">
      <Button variant="ghost" size="sm" className="w-fit gap-1.5 text-muted-foreground" onClick={() => router.push("/courses")}>
        <ArrowLeft className="size-4" />
        Voltar para cursos
      </Button>

      <div className="flex flex-col gap-4 border-b border-border pb-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex min-w-0 flex-1 gap-4">
            <div className="relative hidden h-24 w-40 shrink-0 overflow-hidden rounded-md border border-border bg-navy-900 sm:block">
              <ApiImage
                src={course.coverImageUrl}
                alt=""
                className="absolute inset-0 size-full object-cover"
                fallbackClassName="absolute inset-0 bg-gradient-to-br from-navy-800 to-navy-950"
              />
            </div>
            <div className="min-w-0 space-y-1.5">
              <div className="flex flex-wrap items-center gap-2.5">
                <h1 className="font-heading text-2xl font-medium tracking-tight sm:text-3xl">
                  {course.title}
                </h1>
                <StatusBadge status={course.status} />
              </div>
              <p className="font-mono text-xs text-muted-foreground">
                /{course.slug} · {course.createdByName}
                {course.workloadHours ? ` · ${course.workloadHours}h` : ""}
                {` · ${modules.length} mód. · ${totalLessons} aulas`}
              </p>
              {canManage && (
                <div className="pt-1">
                  <label className="inline-flex cursor-pointer">
                    <input
                      type="file"
                      accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
                      className="sr-only"
                      disabled={uploadingCover}
                      onChange={(e) => {
                        const file = e.target.files?.[0];
                        e.target.value = "";
                        if (file) void uploadCourseCover(file);
                      }}
                    />
                    <span className="text-xs font-medium text-primary hover:text-primary-hover">
                      {uploadingCover
                        ? "Enviando capa…"
                        : course.coverImageUrl
                          ? "Trocar capa do curso"
                          : "Adicionar capa do curso"}
                    </span>
                  </label>
                  <p className="mt-0.5 text-[11px] text-subtle-foreground">
                    JPG, PNG ou WebP · aparece no dashboard e em Meus cursos
                  </p>
                </div>
              )}
            </div>
          </div>
          {canManage && (
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" size="sm" className="gap-1.5" onClick={openEditDialog}>
                <Pencil className="size-3.5" />
                Editar
              </Button>
              {course.status !== "ARCHIVED" && course.status !== "PUBLISHED" && (
                <Button size="sm" className="gap-1.5" disabled={pending} onClick={() => handleStatusAction("publish")}>
                  <Eye className="size-3.5" />
                  Publicar
                </Button>
              )}
              {course.status === "PUBLISHED" && (
                <Button variant="outline" size="sm" className="gap-1.5" disabled={pending} onClick={() => handleStatusAction("unpublish")}>
                  <EyeOff className="size-3.5" />
                  Despublicar
                </Button>
              )}
              {course.status !== "ARCHIVED" && (
                <Button variant="outline" size="sm" className="gap-1.5" disabled={pending} onClick={() => handleStatusAction("archive")}>
                  <Archive className="size-3.5" />
                  Arquivar
                </Button>
              )}
            </div>
          )}
        </div>
        {course.description && (
          <p className="max-w-2xl text-sm leading-relaxed text-muted-foreground">{course.description}</p>
        )}
      </div>

      <div className="flex flex-col gap-4">
        <div className="flex items-center justify-between">
          <h2 className="font-heading text-lg font-medium tracking-tight">Estrutura curricular</h2>
          {canManage && (
            <Dialog open={moduleDialogOpen} onOpenChange={setModuleDialogOpen}>
              <DialogTrigger asChild>
                <Button size="sm" variant="outline" className="gap-1.5">
                  <Plus className="size-3.5" />
                  Novo módulo
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-md">
                <DialogHeader>
                  <DialogTitle>Novo módulo</DialogTitle>
                  <DialogDescription>Módulos organizam as aulas do curso em blocos.</DialogDescription>
                </DialogHeader>
                <form onSubmit={moduleForm.handleSubmit(onCreateModule)} className="flex flex-col gap-4">
                  <div className="flex flex-col gap-2">
                    <Label htmlFor="module-title">Título</Label>
                    <Input id="module-title" {...moduleForm.register("title")} />
                    {moduleForm.formState.errors.title && (
                      <p className="text-sm text-destructive">{moduleForm.formState.errors.title.message}</p>
                    )}
                  </div>
                  <div className="flex flex-col gap-2">
                    <Label htmlFor="module-description">Descrição</Label>
                    <Textarea id="module-description" rows={3} {...moduleForm.register("description")} />
                  </div>
                  <DialogFooter>
                    <Button type="submit" disabled={moduleForm.formState.isSubmitting}>
                      Criar módulo
                    </Button>
                  </DialogFooter>
                </form>
              </DialogContent>
            </Dialog>
          )}
        </div>

        {modules.length > 0 && (
          <div className="flex items-start gap-2.5 rounded-md border border-dashed border-border/70 px-3.5 py-2.5 text-xs text-muted-foreground">
            <Info className="mt-0.5 size-3.5 shrink-0 text-accent" />
            Em cada módulo você pode anexar vídeo às aulas e montar o exercício manual (4
            alternativas, 1 correta). Alunos matriculados respondem em &quot;Meus cursos&quot;.
          </div>
        )}

        {modules.length === 0 ? (
          <div className="rounded-lg border border-dashed border-border/70 py-10 text-center text-sm text-muted-foreground">
            Nenhum módulo criado ainda.
          </div>
        ) : (
          <ul className="flex flex-col gap-2">
            {modules.map((module, moduleIndex) => {
              const collapsed = collapsedModules[module.id];
              return (
                <li key={module.id} className="rounded-md border border-border bg-surface">
                  <div className="flex items-center justify-between gap-2 px-3 py-2.5">
                    <button
                      type="button"
                      onClick={() => toggleModule(module.id)}
                      className="flex min-w-0 flex-1 items-center gap-2.5 text-left"
                    >
                      <ChevronDown
                        className={cn(
                          "size-4 shrink-0 text-muted-foreground transition-transform",
                          collapsed && "-rotate-90",
                        )}
                      />
                      <span className="min-w-0 truncate font-medium">{module.title}</span>
                      <StatusBadge status={module.status} />
                      <span className="hidden shrink-0 text-xs text-muted-foreground sm:inline">
                        {module.lessons.length} {module.lessons.length === 1 ? "aula" : "aulas"}
                      </span>
                    </button>
                    {canManage && (
                      <div className="flex shrink-0 items-center gap-0.5">
                        <Button variant="ghost" size="icon-sm" disabled={moduleIndex === 0} onClick={() => moveModule(module, -1)}>
                          <ArrowUp className="size-3.5" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          disabled={moduleIndex === modules.length - 1}
                          onClick={() => moveModule(module, 1)}
                        >
                          <ArrowDown className="size-3.5" />
                        </Button>
                        <DropdownMenu>
                          <DropdownMenuTrigger asChild>
                            <Button variant="ghost" size="icon-sm">
                              <MoreHorizontal className="size-3.5" />
                            </Button>
                          </DropdownMenuTrigger>
                          <DropdownMenuContent align="end">
                            {module.status === "DRAFT" && (
                              <DropdownMenuItem onClick={() => publishModule(module)}>
                                <Eye className="size-3.5" />
                                Publicar módulo
                              </DropdownMenuItem>
                            )}
                            <DropdownMenuItem variant="destructive" onClick={() => deleteModule(module)}>
                              <Trash2 className="size-3.5" />
                              Excluir módulo
                            </DropdownMenuItem>
                          </DropdownMenuContent>
                        </DropdownMenu>
                      </div>
                    )}
                  </div>

                  {!collapsed && (
                    <div className="flex flex-col gap-3 border-t border-border/70 px-4 py-3">
                      {module.description && (
                        <p className="text-sm text-muted-foreground">{module.description}</p>
                      )}

                      {canManage && <ModuleQuizPanel module={module} />}

                      {module.lessons.length === 0 ? (
                        <p className="text-sm text-muted-foreground">Nenhuma aula neste módulo.</p>
                      ) : (
                        <ul className="flex flex-col divide-y divide-border/70 rounded-md border border-border/70">
                          {module.lessons.map((lesson, lessonIndex) => (
                            <li key={lesson.id} className="flex items-center justify-between gap-2 px-3 py-2.5">
                              <input
                                id={`lesson-thumb-${lesson.id}`}
                                type="file"
                                accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
                                className="sr-only"
                                onChange={(e) => {
                                  const file = e.target.files?.[0];
                                  e.target.value = "";
                                  if (file) void uploadLessonThumbnail(lesson, file);
                                }}
                              />
                              <div className="flex min-w-0 flex-wrap items-center gap-2">
                                <span className="truncate text-sm font-medium">{lesson.title}</span>
                                <StatusBadge status={lesson.status} />
                                <Badge variant="outline" className="text-[10px]">
                                  {lesson.accessType === "FREE_PREVIEW" ? "Prévia grátis" : "Somente matriculados"}
                                </Badge>
                                <span className="text-xs text-muted-foreground">
                                  {lesson.currentVideoAssetId
                                    ? formatDuration(lesson.durationSeconds) ?? "com vídeo"
                                    : "sem vídeo"}
                                </span>
                                {uploadingLessonId === lesson.id && (
                                  <span className="text-xs text-primary">enviando…</span>
                                )}
                              </div>
                              {canManage && (
                                <div className="flex shrink-0 items-center gap-0.5">
                                  <Button
                                    variant="ghost"
                                    size="icon-sm"
                                    disabled={lessonIndex === 0}
                                    onClick={() => moveLesson(module, lesson, -1)}
                                  >
                                    <ArrowUp className="size-3.5" />
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="icon-sm"
                                    disabled={lessonIndex === module.lessons.length - 1}
                                    onClick={() => moveLesson(module, lesson, 1)}
                                  >
                                    <ArrowDown className="size-3.5" />
                                  </Button>
                                  <DropdownMenu>
                                    <DropdownMenuTrigger asChild>
                                      <Button variant="ghost" size="icon-sm">
                                        <MoreHorizontal className="size-3.5" />
                                      </Button>
                                    </DropdownMenuTrigger>
                                    <DropdownMenuContent align="end">
                                      <DropdownMenuItem
                                        onSelect={(e) => {
                                          e.preventDefault();
                                          setVideoUploadLesson(lesson);
                                        }}
                                      >
                                        <Video className="size-3.5" />
                                        {lesson.currentVideoAssetId ? "Substituir vídeo" : "Enviar vídeo"}
                                      </DropdownMenuItem>
                                      <DropdownMenuItem
                                        disabled={!lesson.currentVideoAssetId || thumbnailLessonId === lesson.id}
                                        onSelect={(e) => {
                                          e.preventDefault();
                                          document
                                            .getElementById(`lesson-thumb-${lesson.id}`)
                                            ?.click();
                                        }}
                                      >
                                        <ImagePlus className="size-3.5" />
                                        {thumbnailLessonId === lesson.id
                                          ? "Enviando thumbnail…"
                                          : "Definir thumbnail da aula"}
                                      </DropdownMenuItem>
                                      <DropdownMenuItem
                                        disabled={pending || !lesson.currentVideoAssetId}
                                        onClick={() => void requestAiGeneration(lesson)}
                                      >
                                        <Cpu className="size-3.5" />
                                        Gerar exercícios com IA
                                      </DropdownMenuItem>
                                      {lesson.status === "DRAFT" && (
                                        <DropdownMenuItem onClick={() => publishLesson(lesson)}>
                                          <Eye className="size-3.5" />
                                          Publicar aula
                                        </DropdownMenuItem>
                                      )}
                                      <DropdownMenuItem variant="destructive" onClick={() => deleteLesson(lesson)}>
                                        <Trash2 className="size-3.5" />
                                        Excluir aula
                                      </DropdownMenuItem>
                                    </DropdownMenuContent>
                                  </DropdownMenu>
                                </div>
                              )}
                            </li>
                          ))}
                        </ul>
                      )}

                      {canManage && (
                        <Dialog
                          open={lessonDialogModuleId === module.id}
                          onOpenChange={(open) => setLessonDialogModuleId(open ? module.id : null)}
                        >
                          <DialogTrigger asChild>
                            <Button variant="outline" size="sm" className="w-fit gap-1.5">
                              <Plus className="size-3.5" />
                              Nova aula
                            </Button>
                          </DialogTrigger>
                          <DialogContent className="sm:max-w-md">
                            <DialogHeader>
                              <DialogTitle>Nova aula</DialogTitle>
                              <DialogDescription>Adicione uma aula ao módulo &quot;{module.title}&quot;.</DialogDescription>
                            </DialogHeader>
                            <form
                              onSubmit={lessonForm.handleSubmit((data) => onCreateLesson(module.id, data))}
                              className="flex flex-col gap-4"
                            >
                              <div className="flex flex-col gap-2">
                                <Label htmlFor="lesson-title">Título</Label>
                                <Input id="lesson-title" {...lessonForm.register("title")} />
                                {lessonForm.formState.errors.title && (
                                  <p className="text-sm text-destructive">{lessonForm.formState.errors.title.message}</p>
                                )}
                              </div>
                              <div className="flex flex-col gap-2">
                                <Label htmlFor="lesson-description">Descrição</Label>
                                <Textarea id="lesson-description" rows={2} {...lessonForm.register("description")} />
                              </div>
                              <div className="grid grid-cols-2 gap-3">
                                <div className="flex flex-col gap-2">
                                  <Label htmlFor="lesson-duration">Duração (segundos)</Label>
                                  <Input id="lesson-duration" type="number" min={0} {...lessonForm.register("durationSeconds")} />
                                </div>
                                <div className="flex flex-col gap-2">
                                  <Label>Acesso</Label>
                                  <Select
                                    defaultValue="ENROLLED_ONLY"
                                    onValueChange={(value) =>
                                      lessonForm.setValue("accessType", value as LessonFormInput["accessType"])
                                    }
                                  >
                                    <SelectTrigger>
                                      <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                      <SelectItem value="ENROLLED_ONLY">Somente matriculados</SelectItem>
                                      <SelectItem value="FREE_PREVIEW">Prévia grátis</SelectItem>
                                    </SelectContent>
                                  </Select>
                                </div>
                              </div>
                              <DialogFooter>
                                <Button type="submit" disabled={lessonForm.formState.isSubmitting}>
                                  Criar aula
                                </Button>
                              </DialogFooter>
                            </form>
                          </DialogContent>
                        </Dialog>
                      )}
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>

      <CourseEnrollmentsPanel courseId={courseId} />

      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Editar curso</DialogTitle>
          </DialogHeader>
          <form onSubmit={editForm.handleSubmit(onEditCourse)} className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
              <Label htmlFor="edit-title">Título</Label>
              <Input id="edit-title" {...editForm.register("title")} />
              {editForm.formState.errors.title && (
                <p className="text-sm text-destructive">{editForm.formState.errors.title.message}</p>
              )}
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="edit-description">Descrição</Label>
              <Textarea id="edit-description" rows={4} {...editForm.register("description")} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="edit-workload">Carga horária (obrigatória)</Label>
              <Input id="edit-workload" type="number" min={0.5} step="0.5" required {...editForm.register("workloadHours")} />
            </div>
            <div className="flex flex-col gap-2">
              <Label htmlFor="edit-price">Preço (R$)</Label>
              <Input
                id="edit-price"
                type="number"
                min={0}
                step="0.01"
                required
                {...editForm.register("priceReais")}
              />
              {editForm.formState.errors.priceReais && (
                <p className="text-sm text-destructive">{editForm.formState.errors.priceReais.message}</p>
              )}
            </div>
            <DialogFooter>
              <Button type="submit" disabled={editForm.formState.isSubmitting}>
                Salvar
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <VideoUploadDialog
        open={videoUploadLesson != null}
        onOpenChange={(open) => {
          if (!open && uploadingLessonId == null) setVideoUploadLesson(null);
        }}
        lessonTitle={videoUploadLesson?.title ?? ""}
        replacing={Boolean(videoUploadLesson?.currentVideoAssetId)}
        pending={videoUploadLesson != null && uploadingLessonId === videoUploadLesson.id}
        progress={
          videoUploadLesson != null && uploadingLessonId === videoUploadLesson.id
            ? uploadProgress
            : null
        }
        onSubmit={async (payload) => {
          if (!videoUploadLesson) return;
          await uploadLessonVideo(videoUploadLesson, payload);
        }}
      />
    </div>
  );
}

export default function CourseDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  return (
    <ProtectedRoute allowedRoles={["SUPER_ADMIN", "INSTRUCTOR"]}>
      <DashboardShell>
        <CourseDetailContent courseId={id} />
      </DashboardShell>
    </ProtectedRoute>
  );
}
