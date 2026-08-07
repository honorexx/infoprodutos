"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { apiFetch, ApiError } from "@/lib/api-client";
import { refreshAccessToken } from "@/lib/api-client";
import { setAccessToken } from "@/lib/token-store";
import type { AuthResponse, MeResponse, RoleCode } from "@/lib/types";

interface AuthUser {
  id: string;
  name: string;
  email: string;
  roles: RoleCode[];
}

interface AuthContextValue {
  user: AuthUser | null;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  hasRole: (...roles: RoleCode[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

function toAuthUser(source: AuthResponse | MeResponse): AuthUser {
  return {
    id: "userId" in source ? source.userId : source.id,
    name: source.name,
    email: source.email,
    roles: source.roles,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      const refreshed = await refreshAccessToken();
      if (!refreshed) {
        if (!cancelled) setIsLoading(false);
        return;
      }
      try {
        const me = await apiFetch<MeResponse>("/auth/me");
        if (!cancelled) setUser(toAuthUser(me));
      } catch {
        if (!cancelled) setUser(null);
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    }

    void bootstrap();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiFetch<AuthResponse>("/auth/login", {
      method: "POST",
      body: { email, password },
      skipAuth: true,
    });
    setAccessToken(response.accessToken);
    setUser(toAuthUser(response));
  }, []);

  const register = useCallback(async (name: string, email: string, password: string) => {
    const response = await apiFetch<AuthResponse>("/auth/register", {
      method: "POST",
      body: { name, email, password },
      skipAuth: true,
    });
    setAccessToken(response.accessToken);
    setUser(toAuthUser(response));
  }, []);

  const logout = useCallback(async () => {
    try {
      await apiFetch<void>("/auth/logout", { method: "POST" });
    } catch (error) {
      // Mesmo que a chamada falhe (ex.: token já expirado), limpamos o estado local.
      if (!(error instanceof ApiError)) {
        console.error("Falha inesperada ao fazer logout", error);
      }
    } finally {
      setAccessToken(null);
      setUser(null);
    }
  }, []);

  const hasRole = useCallback(
    (...roles: RoleCode[]) => (user ? roles.some((role) => user.roles.includes(role)) : false),
    [user],
  );

  const value = useMemo<AuthContextValue>(
    () => ({ user, isLoading, login, register, logout, hasRole }),
    [user, isLoading, login, register, logout, hasRole],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth deve ser usado dentro de um AuthProvider");
  }
  return context;
}
