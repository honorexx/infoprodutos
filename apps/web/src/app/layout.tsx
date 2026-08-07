import type { Metadata } from "next";
import { Fraunces, Public_Sans, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Providers } from "@/components/providers";
import { SiteHeader } from "@/components/site-header";
import { Toaster } from "@/components/ui/sonner";

const heading = Fraunces({
  variable: "--font-heading",
  subsets: ["latin"],
  style: ["normal", "italic"],
  axes: ["opsz", "SOFT"],
});

const sans = Public_Sans({
  variable: "--font-sans",
  subsets: ["latin"],
});

const mono = Geist_Mono({
  variable: "--font-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: {
    default: "PKS Consultoria — Educação digital com estrutura profissional",
    template: "%s · PKS Consultoria",
  },
  description:
    "Plataforma própria de cursos online da PKS Consultoria, com painel administrativo, área do professor e área do aluno.",
  icons: {
    icon: [
      { url: "/brand/favicon-pks.png?v=3", type: "image/png", sizes: "512x512" },
      { url: "/brand/favicon-32.png?v=3", type: "image/png", sizes: "32x32" },
      { url: "/favicon.ico?v=3", sizes: "any" },
    ],
    shortcut: "/favicon.ico?v=3",
    apple: [{ url: "/brand/apple-touch-icon.png?v=3", sizes: "180x180" }],
  },
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="pt-BR"
      className={`${heading.variable} ${sans.variable} ${mono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <body className="min-h-full flex flex-col bg-background text-foreground">
        <Providers>
          <SiteHeader />
          <main className="flex flex-1 flex-col">{children}</main>
          <Toaster />
        </Providers>
      </body>
    </html>
  );
}
