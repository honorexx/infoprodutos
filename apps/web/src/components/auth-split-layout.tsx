"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import Image from "next/image";

const ease = [0.22, 1, 0.36, 1] as const;
const MONO_GOLD = "/brand/pks-monogram.png";

/**
 * Uma composição: marca e formulário se encontram no centro da tela,
 * sobre o mesmo navy — sem “vazio” no meio nem painéis competindo.
 */
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
    <div className="relative grid min-h-[100dvh] flex-1 bg-navy-950 lg:grid-cols-2">
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_70%_55%_at_35%_40%,rgba(186,147,100,0.16),transparent_55%),radial-gradient(ellipse_40%_35%_at_78%_70%,rgba(186,147,100,0.06),transparent_50%)]"
      />

      <aside className="relative z-10 hidden lg:flex lg:flex-col lg:justify-center lg:py-16">
        <div className="ml-auto w-full max-w-[24rem] px-10 xl:px-14">
          <Link href="/" aria-label="PKS Consultoria — início" className="block w-fit">
            <motion.div
              initial={{ opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, ease }}
            >
              <Image
                src={MONO_GOLD}
                alt="PKS"
                width={360}
                height={182}
                sizes="400px"
                quality={100}
                priority
                unoptimized
                className="h-[min(20vh,8.75rem)] w-auto max-w-full object-contain object-left xl:h-[min(22vh,9.5rem)]"
              />
            </motion.div>
          </Link>

          <motion.div
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1, ease }}
            className="mt-11"
          >
            <p className="text-xs font-semibold tracking-[0.16em] text-accent uppercase">
              {kicker}
            </p>
            <h2 className="mt-4 text-balance font-heading text-[2.05rem] leading-[1.12] font-medium tracking-tight xl:text-[2.35rem]">
              {title}
            </h2>
            <p className="mt-5 max-w-[20rem] text-pretty text-[0.95rem] leading-relaxed text-slate-300/85">
              {description}
            </p>
          </motion.div>

          <p className="mt-16 text-xs tracking-[0.08em] text-slate-500 uppercase">
            © {new Date().getFullYear()} PKS Consultoria
          </p>
        </div>
      </aside>

      <section className="relative z-10 flex flex-1 items-center lg:py-16">
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.06, ease }}
          className="mx-auto w-full max-w-[22.5rem] px-6 py-14 sm:px-8 lg:mx-0 lg:px-10 xl:px-14"
        >
          <Link href="/" className="mb-10 block w-fit lg:hidden" aria-label="PKS Consultoria">
            <Image
              src={MONO_GOLD}
              alt="PKS"
              width={160}
              height={81}
              sizes="180px"
              quality={100}
              priority
              unoptimized
              className="h-11 w-auto object-contain"
            />
          </Link>
          {children}
        </motion.div>
      </section>
    </div>
  );
}
