import { Badge, type badgeVariants } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { VariantProps } from "class-variance-authority";

type BadgeVariant = VariantProps<typeof badgeVariants>["variant"];

const STATUS_CONFIG: Record<string, { label: string; variant: BadgeVariant }> = {
  // Curso / módulo / aula
  DRAFT: { label: "Rascunho", variant: "secondary" },
  PUBLISHED: { label: "Publicado", variant: "success" },
  ARCHIVED: { label: "Arquivado", variant: "destructive" },
  APPROVED: { label: "Aprovada", variant: "success" },
  REJECTED: { label: "Rejeitada", variant: "destructive" },

  // Dificuldade
  EASY: { label: "Fácil", variant: "secondary" },
  MEDIUM: { label: "Média", variant: "info" },
  HARD: { label: "Difícil", variant: "warning" },

  // Processamento de IA
  PENDING: { label: "Na fila", variant: "secondary" },
  TRANSCRIBING: { label: "Transcrevendo", variant: "info" },
  TRANSCRIBED: { label: "Transcrito", variant: "info" },
  GENERATING: { label: "Gerando questões", variant: "info" },
  AWAITING_REVIEW: { label: "Aguardando revisão", variant: "warning" },
  COMPLETED: { label: "Concluído", variant: "success" },
  FAILED: { label: "Falhou", variant: "destructive" },
  CANCELLED: { label: "Cancelado", variant: "secondary" },
};

export function StatusBadge({ status, className }: { status: string; className?: string }) {
  const config = STATUS_CONFIG[status] ?? { label: status, variant: "secondary" as const };
  return (
    <Badge variant={config.variant} className={cn(className)}>
      {config.label}
    </Badge>
  );
}
