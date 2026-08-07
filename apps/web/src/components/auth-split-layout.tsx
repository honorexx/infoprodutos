import Link from "next/link";
import { BadgeCheck } from "lucide-react";
import { Logo } from "@/components/logo";

const PANEL_HIGHLIGHTS = [
  "Papéis com fronteiras claras entre admin, professor e aluno.",
  "Estrutura curricular publicada apenas quando estiver pronta.",
  "Segurança e auditoria construídas desde a fundação.",
];

export function AuthSplitLayout({
  kicker,
  title,
  description,
  children,
}: {
  kicker: string;
  title: string;
  description: string;
  children: React.ReactNode;
}) {
  return (
    <div className="grid flex-1 lg:grid-cols-2">
      <div className="relative hidden flex-col justify-between overflow-hidden bg-navy-950 px-10 py-10 text-foreground lg:flex xl:px-16">
        <Link href="/">
          <Logo markClassName="bg-navy-850 text-foreground" />
        </Link>
        <div className="relative flex flex-col gap-5">
          <span className="kicker text-accent">{kicker}</span>
          <h2 className="text-balance font-heading text-3xl leading-tight font-medium tracking-tight xl:text-4xl">
            {title}
          </h2>
          <p className="max-w-sm text-sm leading-relaxed text-muted-foreground">{description}</p>
          <ul className="mt-2 flex flex-col gap-3">
            {PANEL_HIGHLIGHTS.map((item) => (
              <li key={item} className="flex items-start gap-2.5 text-sm text-slate-300">
                <BadgeCheck className="mt-0.5 size-4 shrink-0 text-primary" />
                {item}
              </li>
            ))}
          </ul>
        </div>
        <p className="relative text-xs text-slate-400">
          © {new Date().getFullYear()} Infoprodutos
        </p>
      </div>

      <div className="flex flex-1 items-center justify-center px-6 py-16 sm:px-10">
        <div className="flex w-full max-w-sm flex-col gap-6">
          <Link href="/" className="lg:hidden">
            <Logo />
          </Link>
          {children}
        </div>
      </div>
    </div>
  );
}
