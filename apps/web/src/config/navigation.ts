import type { LucideIcon } from "lucide-react";
import {
  Award,
  BookOpen,
  Compass,
  Cpu,
  GraduationCap,
  LayoutDashboard,
  Package,
  Settings,
  Users,
} from "lucide-react";
import type { RoleCode } from "@/lib/types";

export interface NavItem {
  href: string;
  label: string;
  icon: LucideIcon;
  /** Papéis autorizados. Omitido = qualquer autenticado. */
  roles?: RoleCode[];
}

/** Navegação principal — apenas rotas funcionais. */
export const primaryNavigation: NavItem[] = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/descobrir", label: "Descobrir", icon: Compass },
  { href: "/my-courses", label: "Meus cursos", icon: GraduationCap, roles: ["STUDENT"] },
  { href: "/my-certificates", label: "Certificados", icon: Award, roles: ["STUDENT"] },
  { href: "/courses", label: "Cursos", icon: BookOpen, roles: ["SUPER_ADMIN", "INSTRUCTOR"] },
  { href: "/admin/packages", label: "Pacotes", icon: Package, roles: ["SUPER_ADMIN"] },
  { href: "/ai", label: "Processamentos de IA", icon: Cpu, roles: ["SUPER_ADMIN", "INSTRUCTOR"] },
  { href: "/admin/users", label: "Usuários", icon: Users, roles: ["SUPER_ADMIN"] },
  { href: "/settings", label: "Configurações", icon: Settings },
];

export function filterNavByRole(items: NavItem[], hasRole: (...roles: RoleCode[]) => boolean): NavItem[] {
  return items.filter((item) => !item.roles || hasRole(...item.roles));
}
