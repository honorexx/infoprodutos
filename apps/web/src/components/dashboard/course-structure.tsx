import Link from "next/link";
import { AppCard } from "@/components/ui/app-card";
import { CourseTree } from "@/components/courses/course-tree";
import type { CourseTreeData } from "@/types/dashboard";

/**
 * Pré-visualização da estrutura de um curso (módulos/aulas). Usa dados de
 * demonstração — o gerenciamento real de cursos acontece em `/courses`.
 */
export function CourseStructure({ course }: { course: CourseTreeData }) {
  return (
    <AppCard tone="surface" className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-medium">Estrutura do curso</h2>
          <p className="text-xs text-muted-foreground">Pré-visualização · {course.title}</p>
        </div>
      </div>

      <CourseTree course={course} />

      <p className="text-xs text-muted-foreground">
        Conteúdo de exemplo.{" "}
        <Link href="/courses" className="font-medium text-primary hover:underline">
          Gerencie seus cursos reais
        </Link>
        .
      </p>
    </AppCard>
  );
}
