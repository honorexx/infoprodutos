"use client";

import { useCallback, useEffect, useId, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { BookOpen, Loader2, Search } from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { apiFetch } from "@/lib/api-client";
import type { CourseSummary, PageResponse } from "@/lib/types";
import { cn } from "@/lib/utils";

/**
 * Busca global de cursos no cabeçalho autenticado.
 * Aluno: cursos em que está matriculado. Professor/admin: cursos do escopo do papel.
 */
export function CourseSearch({ className }: { className?: string }) {
  const { user, hasRole } = useAuth();
  const router = useRouter();
  const listId = useId();
  const rootRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const [query, setQuery] = useState("");
  const [results, setResults] = useState<CourseSummary[]>([]);
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);

  const isStudentOnly = Boolean(user && hasRole("STUDENT") && !hasRole("INSTRUCTOR") && !hasRole("SUPER_ADMIN"));

  const goToCourse = useCallback(
    (course: CourseSummary) => {
      setOpen(false);
      setQuery("");
      setResults([]);
      if (isStudentOnly) {
        router.push(`/my-courses/${course.id}`);
      } else {
        router.push(`/courses/${course.id}`);
      }
    },
    [isStudentOnly, router],
  );

  useEffect(() => {
    const q = query.trim();
    if (q.length < 2) {
      setResults([]);
      setLoading(false);
      setActiveIndex(-1);
      return;
    }

    let cancelled = false;
    const timer = window.setTimeout(async () => {
      setLoading(true);
      try {
        const page = await apiFetch<PageResponse<CourseSummary>>(
          `/courses?q=${encodeURIComponent(q)}&size=8&sort=title,asc`,
        );
        if (cancelled) return;
        const needle = q.toLocaleLowerCase("pt-BR");
        const filtered = page.content.filter((course) => titleMatchesQuery(course.title, needle));
        setResults(filtered);
        setOpen(true);
        setActiveIndex(filtered.length > 0 ? 0 : -1);
      } catch {
        if (cancelled) return;
        setResults([]);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }, 250);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [query]);

  useEffect(() => {
    function onPointerDown(event: MouseEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    function onKeyDown(event: KeyboardEvent) {
      const isModK = (event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k";
      if (isModK) {
        event.preventDefault();
        inputRef.current?.focus();
        inputRef.current?.select();
        setOpen(query.trim().length >= 2);
      }
    }
    document.addEventListener("mousedown", onPointerDown);
    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
      document.removeEventListener("keydown", onKeyDown);
    };
  }, [query]);

  function onInputKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Escape") {
      setOpen(false);
      inputRef.current?.blur();
      return;
    }
    if (!open || results.length === 0) {
      if (event.key === "Enter") event.preventDefault();
      return;
    }
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setActiveIndex((i) => (i + 1) % results.length);
    } else if (event.key === "ArrowUp") {
      event.preventDefault();
      setActiveIndex((i) => (i <= 0 ? results.length - 1 : i - 1));
    } else if (event.key === "Enter") {
      event.preventDefault();
      const course = results[activeIndex] ?? results[0];
      if (course) goToCourse(course);
    }
  }

  const showPanel = open && query.trim().length >= 2;

  return (
    <div ref={rootRef} className={cn("relative min-w-0 flex-1", className)}>
      <label className="relative block">
        <span className="sr-only">Buscar cursos</span>
        <Search className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          ref={inputRef}
          type="search"
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            if (event.target.value.trim().length >= 2) setOpen(true);
          }}
          onFocus={() => {
            if (query.trim().length >= 2) setOpen(true);
          }}
          onKeyDown={onInputKeyDown}
          placeholder="Buscar cursos pelo título…"
          autoComplete="off"
          role="combobox"
          aria-expanded={showPanel}
          aria-controls={listId}
          aria-autocomplete="list"
          className="h-9 w-full rounded-md border border-border bg-surface pr-12 pl-9 text-sm outline-none placeholder:text-muted-foreground focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/25"
        />
        <kbd className="pointer-events-none absolute top-1/2 right-2.5 hidden -translate-y-1/2 rounded border border-border bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground sm:inline">
          ⌘K
        </kbd>
      </label>

      {showPanel && (
        <div
          id={listId}
          role="listbox"
          className="absolute top-[calc(100%+0.35rem)] right-0 left-0 z-50 overflow-hidden rounded-md border border-border bg-surface shadow-elevated"
        >
          {loading && (
            <p className="flex items-center gap-2 px-3 py-2.5 text-sm text-muted-foreground">
              <Loader2 className="size-3.5 animate-spin" /> Buscando…
            </p>
          )}
          {!loading && results.length === 0 && (
            <p className="px-3 py-2.5 text-sm text-muted-foreground">Nenhum curso encontrado.</p>
          )}
          {!loading &&
            results.map((course, index) => (
              <button
                key={course.id}
                type="button"
                role="option"
                aria-selected={index === activeIndex}
                className={cn(
                  "flex w-full items-start gap-2.5 px-3 py-2.5 text-left transition-colors",
                  index === activeIndex ? "bg-primary-soft" : "hover:bg-surface-elevated",
                )}
                onMouseEnter={() => setActiveIndex(index)}
                onClick={() => goToCourse(course)}
              >
                <BookOpen className="mt-0.5 size-4 shrink-0 text-accent" />
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-medium text-foreground">{course.title}</span>
                  <span className="mt-0.5 block text-[11px] text-muted-foreground">
                    {statusLabel(course.status)}
                    {course.createdByName ? ` · ${course.createdByName}` : ""}
                  </span>
                </span>
              </button>
            ))}
        </div>
      )}
    </div>
  );
}

function statusLabel(status: CourseSummary["status"]) {
  switch (status) {
    case "PUBLISHED":
      return "Publicado";
    case "DRAFT":
      return "Rascunho";
    case "ARCHIVED":
      return "Arquivado";
    default:
      return status;
  }
}

/** Prefixo do título ou de qualquer palavra (mesma regra do backend). */
function titleMatchesQuery(title: string, needleLower: string): boolean {
  const normalized = title.trim().toLocaleLowerCase("pt-BR");
  if (normalized.startsWith(needleLower)) return true;
  return normalized.split(/\s+/).some((word) => word.startsWith(needleLower));
}
