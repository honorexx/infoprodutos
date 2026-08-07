"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { ChevronDown } from "lucide-react";
import { cn } from "@/lib/utils";
import { expandCollapse } from "@/lib/animations";
import { StatusBadge } from "@/components/status-badge";
import { LessonRow } from "@/components/courses/lesson-row";
import type { ModuleNode, LessonNode } from "@/types/dashboard";

export function ModuleRow({
  module: moduleNode,
  defaultOpen = false,
  onSelectLesson,
}: {
  module: ModuleNode;
  defaultOpen?: boolean;
  onSelectLesson?: (lesson: LessonNode) => void;
}) {
  const [open, setOpen] = useState(defaultOpen);
  const contentId = `module-${moduleNode.id}-lessons`;

  return (
    <div className="rounded-lg border border-border/70">
      <button
        type="button"
        onClick={() => setOpen((v) => !v)}
        aria-expanded={open}
        aria-controls={contentId}
        className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2.5 text-left transition-colors hover:bg-muted/40"
      >
        <motion.span
          animate={{ rotate: open ? 0 : -90 }}
          transition={{ duration: 0.18 }}
          className="text-muted-foreground"
        >
          <ChevronDown className="size-4" />
        </motion.span>
        <span className="min-w-0 flex-1 truncate text-sm font-medium">{moduleNode.title}</span>
        <span className="hidden text-xs text-muted-foreground sm:inline">
          {moduleNode.lessons.length} {moduleNode.lessons.length === 1 ? "aula" : "aulas"}
        </span>
        <StatusBadge status={moduleNode.status} />
      </button>

      <motion.div
        id={contentId}
        initial={false}
        animate={open ? "expanded" : "collapsed"}
        variants={expandCollapse}
        className={cn("overflow-hidden", open && "border-t border-border/70")}
      >
        <div className="flex flex-col gap-0.5 p-2">
          {moduleNode.lessons.map((lesson) => (
            <LessonRow key={lesson.id} lesson={lesson} onSelect={onSelectLesson} />
          ))}
        </div>
      </motion.div>
    </div>
  );
}
