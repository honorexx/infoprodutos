import { CheckCircle2, Circle, Lock, MoreHorizontal, PlayCircle } from "lucide-react";
import { cn } from "@/lib/utils";
import type { LessonNode } from "@/types/dashboard";

/**
 * Linha de aula reutilizável (preview do dashboard e, futuramente, o
 * construtor de curso real). Presentacional — recebe dados e callbacks por
 * propriedade, sem lógica de negócio própria.
 */
export function LessonRow({
  lesson,
  onSelect,
  onOpenMenu,
}: {
  lesson: LessonNode;
  onSelect?: (lesson: LessonNode) => void;
  onOpenMenu?: (lesson: LessonNode) => void;
}) {
  const StatusIcon = lesson.completed ? CheckCircle2 : lesson.accessType === "FREE" ? PlayCircle : Circle;

  return (
    <div
      role={onSelect ? "button" : undefined}
      tabIndex={onSelect ? 0 : undefined}
      onClick={onSelect ? () => onSelect(lesson) : undefined}
      onKeyDown={
        onSelect
          ? (e) => {
              if (e.key === "Enter" || e.key === " ") onSelect(lesson);
            }
          : undefined
      }
      className={cn(
        "group flex items-center gap-2.5 rounded-md px-2.5 py-2 text-sm transition-colors",
        onSelect && "cursor-pointer hover:bg-muted/50",
      )}
    >
      <StatusIcon
        className={cn(
          "size-4 shrink-0",
          lesson.completed ? "text-success" : "text-muted-foreground/60",
        )}
      />
      <span className="min-w-0 flex-1 truncate">{lesson.title}</span>
      {lesson.accessType === "FREE" && (
        <span className="hidden shrink-0 text-[10px] font-semibold tracking-wide text-accent uppercase sm:inline">
          Gratuita
        </span>
      )}
      <span className="shrink-0 text-xs text-muted-foreground">{lesson.durationLabel}</span>
      {onOpenMenu ? (
        <button
          type="button"
          aria-label="Mais ações"
          onClick={(e) => {
            e.stopPropagation();
            onOpenMenu(lesson);
          }}
          className="shrink-0 rounded-md p-1 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100 hover:bg-muted hover:text-foreground"
        >
          <MoreHorizontal className="size-4" />
        </button>
      ) : (
        <Lock className="size-3.5 shrink-0 text-transparent" aria-hidden="true" />
      )}
    </div>
  );
}
