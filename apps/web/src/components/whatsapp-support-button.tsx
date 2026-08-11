"use client";

import { MessageCircle } from "lucide-react";
import { SUPPORT_WHATSAPP } from "@/lib/support";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export function WhatsAppSupportButton({
  className,
  variant = "outline",
  size = "default",
  label = "Suporte no WhatsApp",
  fullWidth = false,
}: {
  className?: string;
  variant?: "outline" | "secondary" | "default" | "ghost";
  size?: "default" | "sm" | "lg";
  label?: string;
  fullWidth?: boolean;
}) {
  return (
    <Button
      asChild
      variant={variant}
      size={size}
      className={cn(fullWidth && "w-full", className)}
    >
      <a
        href={SUPPORT_WHATSAPP.href}
        target="_blank"
        rel="noopener noreferrer"
        aria-label={`Abrir WhatsApp ${SUPPORT_WHATSAPP.display}`}
      >
        <MessageCircle className="size-4" />
        {label}
      </a>
    </Button>
  );
}
