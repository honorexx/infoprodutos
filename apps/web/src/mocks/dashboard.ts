import { Award, BookOpen, GraduationCap, Star, TrendingUp, Users } from "lucide-react";
import type {
  ActivityItem,
  AiProcessingItem,
  CourseTreeData,
  Metric,
  SecondaryMetric,
} from "@/types/dashboard";

/**
 * DADOS DE DEMONSTRAÇÃO — usados apenas para compor visualmente o dashboard
 * enquanto as funcionalidades reais (matrícula, IA, certificados) não existem
 * (ver Fases 3–5 do roadmap). Nada aqui deve ser tratado como métrica real de
 * produção. Quando a API correspondente existir, estes mocks devem ser
 * substituídos por dados vindos de `apiFetch`.
 */

export const dashboardMetrics: Metric[] = [
  {
    id: "active-students",
    label: "Alunos ativos",
    value: "2.450",
    change: 12.4,
    trend: [4, 6, 5, 8, 7, 9, 12],
    icon: GraduationCap,
  },
  {
    id: "published-courses",
    label: "Cursos publicados",
    value: "38",
    change: 4.1,
    trend: [3, 3, 4, 4, 5, 5, 6],
    icon: BookOpen,
  },
  {
    id: "completions",
    label: "Conclusões no mês",
    value: "612",
    change: 8.7,
    trend: [2, 4, 3, 6, 7, 8, 9],
    icon: TrendingUp,
  },
  {
    id: "certificates",
    label: "Certificados emitidos",
    value: "394",
    change: 5.3,
    trend: [1, 2, 3, 3, 4, 5, 6],
    icon: Award,
  },
];

export const secondaryMetrics: SecondaryMetric[] = [
  {
    id: "new-students",
    label: "Novos alunos (30 dias)",
    value: "186",
    helper: "+22 em relação ao mês anterior",
    icon: Users,
  },
  {
    id: "completion-rate",
    label: "Taxa média de conclusão",
    value: "68%",
    icon: TrendingUp,
  },
  {
    id: "average-rating",
    label: "Avaliação média",
    value: "4.8",
    helper: "de 5 · 1.240 avaliações",
    icon: Star,
  },
];

export const aiProcessingItems: AiProcessingItem[] = [
  {
    id: "job-1",
    title: "Introdução ao Marketing Digital",
    description: "Aula 3 · Funil de vendas na prática",
    progress: 100,
    status: "COMPLETED",
  },
  {
    id: "job-2",
    title: "Copywriting para Infoprodutos",
    description: "Aula 5 · Gatilhos mentais",
    progress: 62,
    status: "TRANSCRIBING",
  },
  {
    id: "job-3",
    title: "Gestão Financeira para Criadores",
    description: "Aula 1 · Precificação de produtos digitais",
    progress: 40,
    status: "GENERATING",
  },
  {
    id: "job-4",
    title: "Edição de Vídeo com Celular",
    description: "Aula 8 · Cortes dinâmicos",
    progress: 88,
    status: "AWAITING_REVIEW",
  },
  {
    id: "job-5",
    title: "SEO para Criadores de Conteúdo",
    description: "Aula 2 · Pesquisa de palavras-chave",
    progress: 0,
    status: "PENDING",
  },
];

export const recentActivity: ActivityItem[] = [
  {
    id: "act-1",
    initials: "MC",
    description: "Publicou uma nova aula",
    courseTitle: "Introdução ao Marketing Digital",
    relativeTime: "há 12 min",
    icon: BookOpen,
  },
  {
    id: "act-2",
    initials: "AS",
    description: "Concluiu o curso",
    courseTitle: "Copywriting para Infoprodutos",
    relativeTime: "há 2h",
    icon: GraduationCap,
  },
  {
    id: "act-3",
    initials: "RP",
    description: "Enviou um novo material de apoio",
    courseTitle: "Gestão Financeira para Criadores",
    relativeTime: "há 5h",
    icon: Award,
  },
  {
    id: "act-4",
    initials: "JL",
    description: "Avaliou o curso com 5 estrelas",
    courseTitle: "Edição de Vídeo com Celular",
    relativeTime: "ontem",
    icon: Star,
  },
];

export const courseTreePreview: CourseTreeData = {
  id: "preview-course",
  title: "Introdução ao Marketing Digital",
  modules: [
    {
      id: "mod-1",
      title: "Módulo 1 · Fundamentos",
      status: "PUBLISHED",
      lessons: [
        { id: "l-1", title: "Boas-vindas ao curso", durationLabel: "4 min", completed: true, accessType: "FREE" },
        { id: "l-2", title: "O que é marketing digital", durationLabel: "11 min", completed: true },
        { id: "l-3", title: "Funil de vendas na prática", durationLabel: "18 min" },
      ],
    },
    {
      id: "mod-2",
      title: "Módulo 2 · Aquisição de audiência",
      status: "PUBLISHED",
      lessons: [
        { id: "l-4", title: "Tráfego orgânico vs. pago", durationLabel: "14 min" },
        { id: "l-5", title: "Construindo uma lista de e-mails", durationLabel: "9 min" },
      ],
    },
    {
      id: "mod-3",
      title: "Módulo 3 · Lançamento (em produção)",
      status: "DRAFT",
      lessons: [{ id: "l-6", title: "Estrutura de um lançamento", durationLabel: "—" }],
    },
  ],
};

export const certificatesIssuedCount = 394;

export const mockNotifications = [
  { id: "n-1", text: "Novo aluno matriculado em Copywriting para Infoprodutos" },
  { id: "n-2", text: "Transcrição da aula \"Gatilhos mentais\" concluída" },
  { id: "n-3", text: "3 cursos aguardando revisão de conteúdo gerado por IA" },
];
