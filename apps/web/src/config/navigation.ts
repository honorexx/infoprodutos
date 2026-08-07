import type { LucideIcon } from "lucide-react";
import {
  Award,
  BookOpen,
  Cpu,
  GraduationCap,
  LayoutDashboard,
  Settings,
  Users,
} from "lucide-react";
import type { RoleCode } from "@/lib/types";

export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
  /** Papéis autorizados a ver o item. Omitido = visível para qualquer usuário autenticado. */
  roles?: RoleCode[];
  /** Item ainda não implementado — some outros marcadores visuais mudam quando true. */
  comingSoon?: boolean;
}

/** Navegação principal — funcionalidades já implementadas. */
export const primaryNavigation: NavItem[] = [
  { href: "/dashboard", label: "Visão geral", icon: LayoutDashboard },
  { href: "/my-courses", label: "Meus cursos", icon: GraduationCap, roles: ["STUDENT"] },
  { href: "/my-certificates", label: "Meus certificados", icon: Award, roles: ["STUDENT"] },
  { href: "/courses", label: "Cursos", icon: BookOpen, roles: ["SUPER_ADMIN", "INSTRUCTOR"] },
  { href: "/admin/users", label: "Usuários", icon: Users, roles: ["SUPER_ADMIN"] },
];

/** Navegação de fases futuras do roadmap — visível, mas desabilitada. */
export const upcomingNavigation: NavItem[] = [
  { href: "/ai", label: "Processamentos de IA", icon: Cpu, roles: ["SUPER_ADMIN", "INSTRUCTOR"] },
  { href: "#", label: "Configurações", icon: Settings, comingSoon: true },
];

export function filterNavByRole(items: NavItem[], hasRole: (...roles: RoleCode[]) => boolean): NavItem[] {
  return items.filter((item) => !item.roles || hasRole(...item.roles));
}
