"use client";

import Link from "next/link";
import {
  ArrowRight,
  BadgeCheck,
  BookOpen,
  CircleDot,
  ClipboardCheck,
  Layers,
  ShieldCheck,
} from "lucide-react";
import { useAuth } from "@/lib/auth-context";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";

const BENEFITS = [
  {
    number: "01",
    title: "Estrutura curricular real",
    description:
      "Cursos organizados em módulos e aulas, com rascunho, publicação e arquivamento — nada fica visível antes da hora certa.",
  },
  {
    number: "02",
    title: "Papéis com fronteiras claras",
    description:
      "Administração, professores e alunos enxergam exatamente o que devem enxergar. Um professor nunca edita o curso de outro.",
  },
  {
    number: "03",
    title: "IA com revisão humana",
    description:
      "Quando o pipeline de exercícios entrar em operação, nenhuma questão gerada automaticamente publica sozinha — sempre passa por aprovação.",
  },
  {
    number: "04",
    title: "Segurança desde a fundação",
    description:
      "Autenticação própria, auditoria de ações administrativas e controle de acesso construídos como base, não como remendo.",
  },
];

const FAQ = [
  {
    question: "A plataforma já está em produção?",
    answer:
      "Ainda não. Estamos construindo em fases pequenas e verificáveis — hoje, a fundação de contas, papéis e a estrutura curricular de cursos já existem.",
  },
  {
    question: "Como funciona a inteligência artificial da plataforma?",
    answer:
      "A IA vai apoiar a criação de exercícios a partir da transcrição das aulas, mas nunca publica nada sozinha: toda questão gerada passa por revisão humana antes de ficar visível ao aluno.",
  },
  {
    question: "Um professor pode editar o curso de outro professor?",
    answer:
      "Não. O acesso de edição é restrito ao dono do curso e à administração — validado tanto na interface quanto no servidor.",
  },
];

function LandingSkeleton() {
  return (
    <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col gap-6 px-6 py-24">
      <Skeleton className="h-4 w-32" />
      <Skeleton className="h-14 w-full max-w-xl" />
      <Skeleton className="h-6 w-full max-w-md" />
    </div>
  );
}

export default function Home() {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return <LandingSkeleton />;
  }

  return (
    <div className="flex flex-1 flex-col">
      {/* HERO */}
      <section className="relative overflow-hidden border-b border-border/70">
        <div className="mx-auto grid w-full max-w-6xl gap-12 px-6 py-20 sm:px-8 sm:py-28 lg:grid-cols-[1.15fr_0.85fr] lg:items-center">
          <div className="flex flex-col gap-6">
            <span className="kicker">Educação digital profissional</span>
            <h1 className="text-balance font-heading text-4xl leading-[1.08] font-medium tracking-tight sm:text-5xl lg:text-[3.4rem]">
              Cursos e infoprodutos com a{" "}
              <span className="italic text-accent">estrutura</span> de uma
              escola de verdade.
            </h1>
            <p className="max-w-lg text-pretty text-base leading-relaxed text-muted-foreground sm:text-lg">
              Uma plataforma própria — não um curso hospedado em outro lugar — com
              painel administrativo, área do professor e área do aluno, construída
              em fases pequenas e revisáveis desde a fundação.
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-3">
              <Link href="/login">
                <Button size="lg" className="gap-1.5">
                  Acessar a plataforma <ArrowRight className="size-4" />
                </Button>
              </Link>
              {!user && (
                <Link href="/register">
                  <Button size="lg" variant="outline">
                    Criar conta
                  </Button>
                </Link>
              )}
            </div>
          </div>

          <CoursePreviewCard />
        </div>
      </section>

      {/* BENEFÍCIOS */}
      <section className="border-b border-border/70">
        <div className="mx-auto grid w-full max-w-6xl gap-10 px-6 py-20 sm:px-8 lg:grid-cols-[0.8fr_1.2fr] lg:gap-16">
          <div className="flex flex-col gap-4">
            <span className="kicker">Por que existe</span>
            <h2 className="text-balance font-heading text-3xl leading-tight font-medium tracking-tight">
              Cada decisão de arquitetura existe para proteger o conteúdo e quem aprende.
            </h2>
          </div>
          <dl className="grid gap-8 sm:grid-cols-2">
            {BENEFITS.map((benefit) => (
              <div key={benefit.number} className="flex flex-col gap-2 border-t border-border/70 pt-4">
                <dt className="flex items-baseline gap-2 font-heading text-sm font-semibold text-accent">
                  {benefit.number}
                </dt>
                <dt className="text-base font-medium tracking-tight">{benefit.title}</dt>
                <dd className="text-sm leading-relaxed text-muted-foreground">{benefit.description}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>

      {/* PARA QUEM */}
      <section className="border-b border-border/70 bg-secondary/40">
        <div className="mx-auto grid w-full max-w-6xl gap-10 px-6 py-20 sm:px-8 lg:grid-cols-2 lg:gap-6">
          <AudienceCard
            icon={BookOpen}
            kicker="Para professores"
            title="Organize o curso como um editor profissional deveria permitir."
            items={[
              "Crie módulos e aulas e reordene a estrutura curricular a qualquer momento.",
              "Controle o que está em rascunho e o que já está publicado.",
              "Convide outros professores para colaborar em um mesmo curso.",
            ]}
          />
          <AudienceCard
            icon={ShieldCheck}
            kicker="Para administradores"
            title="Visibilidade e controle sobre contas, papéis e cursos."
            items={[
              "Gerencie contas, bloqueios e atribuição de papéis.",
              "Acompanhe todos os cursos da plataforma, não só os seus.",
              "Toda ação sensível fica registrada em auditoria.",
            ]}
          />
        </div>
      </section>

      {/* DEPOIMENTOS — honesto: nada é inventado aqui */}
      <section className="border-b border-border/70">
        <div className="mx-auto flex w-full max-w-3xl flex-col items-center gap-4 px-6 py-20 text-center sm:px-8">
          <span className="kicker">Depoimentos</span>
          <p className="text-balance font-heading text-2xl leading-snug font-medium tracking-tight text-muted-foreground italic">
            “Os primeiros depoimentos de alunos e professores aparecerão aqui assim
            que a plataforma estiver em operação.”
          </p>
          <Badge variant="secondary" className="mt-2">
            Espaço reservado
          </Badge>
        </div>
      </section>

      {/* FAQ */}
      <section>
        <div className="mx-auto grid w-full max-w-6xl gap-10 px-6 py-20 sm:px-8 lg:grid-cols-[0.7fr_1.3fr]">
          <div className="flex flex-col gap-4">
            <span className="kicker">Perguntas frequentes</span>
            <h2 className="font-heading text-3xl leading-tight font-medium tracking-tight">
              O essencial, direto ao ponto.
            </h2>
          </div>
          <div className="flex flex-col divide-y divide-border/70">
            {FAQ.map((item) => (
              <details key={item.question} className="group py-5 first:pt-0">
                <summary className="flex cursor-pointer list-none items-center justify-between gap-4 text-base font-medium tracking-tight">
                  {item.question}
                  <CircleDot className="size-3.5 shrink-0 text-accent transition-transform group-open:rotate-45" />
                </summary>
                <p className="mt-3 max-w-2xl text-sm leading-relaxed text-muted-foreground">{item.answer}</p>
              </details>
            ))}
          </div>
        </div>
      </section>

      <footer className="border-t border-border/70">
        <div className="mx-auto flex w-full max-w-6xl flex-col gap-2 px-6 py-8 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between sm:px-8">
          <p>© {new Date().getFullYear()} Infoprodutos. Plataforma em construção por fases.</p>
          <p>Feito com Next.js e Spring Boot.</p>
        </div>
      </footer>
    </div>
  );
}

function AudienceCard({
  icon: Icon,
  kicker,
  title,
  items,
}: {
  icon: React.ComponentType<{ className?: string }>;
  kicker: string;
  title: string;
  items: string[];
}) {
  return (
    <div className="flex flex-col gap-5 rounded-lg border border-border/70 bg-card p-7 sm:p-8">
      <span className="flex size-9 items-center justify-center rounded-md bg-accent/12 text-accent">
        <Icon className="size-4.5" />
      </span>
      <div className="flex flex-col gap-2">
        <span className="kicker">{kicker}</span>
        <h3 className="text-balance font-heading text-xl leading-snug font-medium tracking-tight">{title}</h3>
      </div>
      <ul className="flex flex-col gap-3">
        {items.map((item) => (
          <li key={item} className="flex items-start gap-2.5 text-sm leading-relaxed text-muted-foreground">
            <BadgeCheck className="mt-0.5 size-4 shrink-0 text-accent" />
            {item}
          </li>
        ))}
      </ul>
    </div>
  );
}

function CoursePreviewCard() {
  return (
    <div className="relative">
      <div className="absolute -top-4 -right-4 hidden size-24 rounded-md border border-gold/40 bg-gold/15 sm:block" aria-hidden="true" />
      <div className="relative flex flex-col gap-4 rounded-lg border border-border/70 bg-card p-5 shadow-lifted">
        <div className="flex items-center justify-between">
          <span className="kicker">Prévia da interface</span>
          <Badge variant="secondary">Exemplo</Badge>
        </div>
        <div className="flex flex-col gap-1">
          <p className="font-heading text-lg leading-snug font-medium tracking-tight">Fundamentos de Marketing Digital</p>
          <p className="text-xs text-muted-foreground">3 módulos · 12 aulas · rascunho</p>
        </div>
        <div className="flex flex-col divide-y divide-border/70 overflow-hidden rounded-md border border-border/70">
          {[
            { title: "Módulo 1 — Introdução", lessons: 4, done: true },
            { title: "Módulo 2 — Estratégia de conteúdo", lessons: 5, done: true },
            { title: "Módulo 3 — Métricas e otimização", lessons: 3, done: false },
          ].map((m) => (
            <div key={m.title} className="flex items-center justify-between gap-3 bg-background px-3 py-2.5">
              <div className="flex items-center gap-2.5">
                <Layers className="size-3.5 text-muted-foreground" />
                <span className="text-sm font-medium">{m.title}</span>
              </div>
              <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
                {m.lessons} aulas
                {m.done ? (
                  <ClipboardCheck className="size-3.5 text-success" />
                ) : (
                  <CircleDot className="size-3.5 text-gold" />
                )}
              </span>
            </div>
          ))}
        </div>
        <p className="text-[11px] leading-relaxed text-muted-foreground">
          Ilustração da estrutura curricular. Os cursos publicados na plataforma aparecem no painel após login.
        </p>
      </div>
    </div>
  );
}
