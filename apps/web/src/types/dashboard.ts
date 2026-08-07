import type { LucideIcon } from "lucide-react";

/** Métrica exibida no painel principal (escuro) do dashboard. */
export type Metric = {
  id: string;
  label: string;
  value: string;
  change?: number;
  trend?: number[];
  icon: LucideIcon;
};

/** Métrica compacta secundária (composição diferente do painel principal). */
export type SecondaryMetric = {
  id: string;
  label: string;
  value: string;
  helper?: string;
  icon: LucideIcon;
};

/**
 * Status de processamento de IA. Mesmos valores usados pelo backend na Fase 3
 * (transcrição/geração de questões). Aqui representados apenas visualmente,
 * com dados de demonstração.
 */
export type AiJobStatus =
  | "PENDING"
  | "TRANSCRIBING"
  | "TRANSCRIBED"
  | "GENERATING"
  | "AWAITING_REVIEW"
  | "COMPLETED"
  | "FAILED"
  | "CANCELLED";

export type AiProcessingItem = {
  id: string;
  title: string;
  description: string;
  progress: number;
  status: AiJobStatus;
};

export type ActivityItem = {
  id: string;
  initials: string;
  description: string;
  courseTitle: string;
  relativeTime: string;
  icon: LucideIcon;
};

export type LessonNode = {
  id: string;
  title: string;
  durationLabel: string;
  completed?: boolean;
  accessType?: "FREE" | "ENROLLED";
};

export type ModuleNode = {
  id: string;
  title: string;
  status: "DRAFT" | "PUBLISHED" | "ARCHIVED";
  lessons: LessonNode[];
};

export type CourseTreeData = {
  id: string;
  title: string;
  modules: ModuleNode[];
};
