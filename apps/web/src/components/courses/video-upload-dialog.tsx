"use client";

import { useEffect, useId, useState } from "react";
import { ImagePlus, Video } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";

const THUMB_ACCEPT = "image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp";
const VIDEO_ACCEPT = "video/mp4,video/webm,video/quicktime,video/*";

function isImageFile(file: File) {
  const type = file.type.toLowerCase();
  if (type === "image/jpeg" || type === "image/png" || type === "image/webp" || type === "image/jpg") {
    return true;
  }
  const name = file.name.toLowerCase();
  return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
}

export type VideoUploadPayload = {
  video: File;
  thumbnail: File;
};

type VideoUploadDialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  lessonTitle: string;
  replacing?: boolean;
  pending?: boolean;
  /** 0–100 enquanto envia; null quando ocioso. */
  progress?: number | null;
  onSubmit: (payload: VideoUploadPayload) => void | Promise<void>;
};

export function VideoUploadDialog({
  open,
  onOpenChange,
  lessonTitle,
  replacing = false,
  pending = false,
  progress = null,
  onSubmit,
}: VideoUploadDialogProps) {
  const videoInputId = useId();
  const thumbInputId = useId();
  const [video, setVideo] = useState<File | null>(null);
  const [thumbnail, setThumbnail] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      setVideo(null);
      setThumbnail(null);
      setError(null);
    }
  }, [open]);

  useEffect(() => {
    if (!thumbnail) {
      setPreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(thumbnail);
    setPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [thumbnail]);

  function onPickVideo(file: File | undefined) {
    setError(null);
    if (!file) return;
    if (!file.type.startsWith("video/") && file.type !== "application/octet-stream") {
      setError("Selecione um arquivo de vídeo (ex.: MP4).");
      return;
    }
    setVideo(file);
  }

  function onPickThumbnail(file: File | undefined) {
    setError(null);
    if (!file) return;
    if (!isImageFile(file)) {
      setError("Thumbnail deve ser JPG, PNG ou WebP.");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError("Thumbnail deve ter no máximo 5 MB.");
      return;
    }
    setThumbnail(file);
  }

  async function handleSubmit() {
    if (!video || !thumbnail) {
      setError("Vídeo e thumbnail são obrigatórios.");
      return;
    }
    setError(null);
    await onSubmit({ video, thumbnail });
  }

  const canSubmit = Boolean(video && thumbnail) && !pending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{replacing ? "Substituir vídeo" : "Enviar vídeo"}</DialogTitle>
          <DialogDescription>
            Aula: {lessonTitle}. A thumbnail é obrigatória e aparece como capa do player.
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label htmlFor={videoInputId}>
              Vídeo <span className="text-danger">*</span>
            </Label>
            <label
              htmlFor={videoInputId}
              className={cn(
                "flex cursor-pointer flex-col items-center justify-center gap-2 rounded-md border border-dashed px-4 py-6 text-center transition-colors",
                video
                  ? "border-border-gold-active bg-primary-soft"
                  : "border-border hover:border-border-gold hover:bg-surface-hover",
              )}
            >
              <Video className="size-5 text-primary" />
              <span className="text-sm font-medium">
                {video ? video.name : "Selecionar arquivo de vídeo"}
              </span>
              <span className="text-xs text-muted-foreground">MP4, WebM ou MOV</span>
            </label>
            <input
              id={videoInputId}
              type="file"
              accept={VIDEO_ACCEPT}
              className="sr-only"
              onChange={(e) => onPickVideo(e.target.files?.[0])}
            />
          </div>

          <div className="flex flex-col gap-2">
            <Label htmlFor={thumbInputId}>
              Thumbnail <span className="text-danger">*</span>
            </Label>
            <label
              htmlFor={thumbInputId}
              className={cn(
                "flex cursor-pointer flex-col items-center justify-center gap-2 overflow-hidden rounded-md border border-dashed px-4 py-6 text-center transition-colors",
                thumbnail
                  ? "border-border-gold-active bg-primary-soft"
                  : "border-border hover:border-border-gold hover:bg-surface-hover",
              )}
            >
              {previewUrl ? (
                // eslint-disable-next-line @next/next/no-img-element -- blob preview local
                <img
                  src={previewUrl}
                  alt="Pré-visualização da thumbnail"
                  className="mb-1 aspect-video w-full max-w-xs rounded-sm object-cover"
                />
              ) : (
                <ImagePlus className="size-5 text-primary" />
              )}
              <span className="text-sm font-medium">
                {thumbnail ? thumbnail.name : "Selecionar imagem de capa"}
              </span>
              <span className="text-xs text-muted-foreground">JPG, PNG ou WebP · máx. 5 MB</span>
            </label>
            <input
              id={thumbInputId}
              type="file"
              accept={THUMB_ACCEPT}
              className="sr-only"
              onChange={(e) => onPickThumbnail(e.target.files?.[0])}
            />
          </div>

          {error && <p className="text-sm text-danger">{error}</p>}

          {pending && (
            <div className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span>Enviando vídeo…</span>
                <span>{progress != null ? `${progress}%` : "preparando"}</span>
              </div>
              <div className="h-1.5 overflow-hidden rounded-full bg-muted">
                <div
                  className="h-full rounded-full bg-primary transition-[width] duration-200"
                  style={{ width: `${progress != null ? Math.max(progress, 2) : 8}%` }}
                />
              </div>
              <p className="text-xs text-muted-foreground">
                Aulas longas podem levar vários minutos. Não feche esta janela.
              </p>
            </div>
          )}
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button type="button" variant="outline" disabled={pending} onClick={() => onOpenChange(false)}>
            Cancelar
          </Button>
          <Button type="button" disabled={!canSubmit} onClick={() => void handleSubmit()}>
            {pending
              ? progress != null
                ? `Enviando… ${progress}%`
                : "Enviando…"
              : replacing
                ? "Substituir"
                : "Enviar"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
