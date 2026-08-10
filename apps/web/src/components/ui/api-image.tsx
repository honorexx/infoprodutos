"use client";

import { useEffect, useState } from "react";
import { API_BASE_URL } from "@/lib/config";
import { refreshAccessToken } from "@/lib/api-client";
import { getAccessToken } from "@/lib/token-store";
import { cn } from "@/lib/utils";

/**
 * Carrega imagem da API com Bearer (capas locais `/courses/{id}/cover`).
 * URLs http(s) externas são usadas direto.
 */
export function ApiImage({
  src,
  alt,
  className,
  fallbackClassName,
}: {
  src: string | null | undefined;
  alt: string;
  className?: string;
  fallbackClassName?: string;
}) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null);
  const [failed, setFailed] = useState(false);

  const isExternal = Boolean(src && /^https?:\/\//i.test(src));
  const apiPath = src && !isExternal ? (src.startsWith("/") ? src : `/${src}`) : null;

  useEffect(() => {
    setFailed(false);
    setObjectUrl(null);
    if (!apiPath) {
      return;
    }

    let cancelled = false;
    let created: string | null = null;

    async function fetchBlob(path: string, retried: boolean): Promise<Blob | null> {
      const headers: Record<string, string> = {};
      const token = getAccessToken();
      if (token) headers.Authorization = `Bearer ${token}`;
      const response = await fetch(`${API_BASE_URL}${path}`, {
        headers,
        credentials: "include",
      });
      if (response.status === 401 && !retried) {
        const ok = await refreshAccessToken();
        if (ok) return fetchBlob(path, true);
      }
      if (!response.ok) return null;
      return response.blob();
    }

    async function load() {
      try {
        const blob = await fetchBlob(apiPath!, false);
        if (cancelled) return;
        if (!blob) {
          setFailed(true);
          return;
        }
        created = URL.createObjectURL(blob);
        setObjectUrl(created);
      } catch {
        if (!cancelled) setFailed(true);
      }
    }

    void load();
    return () => {
      cancelled = true;
      if (created) URL.revokeObjectURL(created);
    };
  }, [apiPath]);

  if (!src || failed) {
    return <div className={cn(fallbackClassName ?? className)} aria-hidden />;
  }

  if (isExternal) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img src={src} alt={alt} className={className} />
    );
  }

  if (!objectUrl) {
    return <div className={cn(fallbackClassName ?? className, "animate-pulse")} aria-hidden />;
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={objectUrl} alt={alt} className={className} />
  );
}
