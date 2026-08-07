import { ModuleRow } from "@/components/courses/module-row";
import type { CourseTreeData, LessonNode } from "@/types/dashboard";

/**
 * Árvore visual curso → módulo → aula, com expandir/recolher por módulo.
 * Reordenação e menus contextuais por aula pertencem a uma fase futura
 * (não implementados aqui, conforme escopo desta etapa visual).
 */
export function CourseTree({
  course,
  onSelectLesson,
}: {
  course: CourseTreeData;
  onSelectLesson?: (lesson: LessonNode) => void;
}) {
  return (
    <div className="flex flex-col gap-2">
      {course.modules.map((moduleNode, index) => (
        <ModuleRow key={moduleNode.id} module={moduleNode} defaultOpen={index === 0} onSelectLesson={onSelectLesson} />
      ))}
    </div>
  );
}
