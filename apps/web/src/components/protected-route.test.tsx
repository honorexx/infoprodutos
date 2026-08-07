import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { ProtectedRoute } from "@/components/protected-route";

const replaceMock = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace: replaceMock, push: vi.fn() }),
}));

const useAuthMock = vi.fn();
vi.mock("@/lib/auth-context", () => ({
  useAuth: () => useAuthMock(),
}));

describe("ProtectedRoute", () => {
  beforeEach(() => {
    replaceMock.mockClear();
    useAuthMock.mockReset();
  });

  it("mostra estado de carregamento enquanto isLoading é true", () => {
    useAuthMock.mockReturnValue({ user: null, isLoading: true, hasRole: () => false });
    render(
      <ProtectedRoute>
        <div>Conteúdo protegido</div>
      </ProtectedRoute>,
    );
    expect(screen.queryByText("Conteúdo protegido")).not.toBeInTheDocument();
  });

  it("redireciona para /login quando não há usuário autenticado", () => {
    useAuthMock.mockReturnValue({ user: null, isLoading: false, hasRole: () => false });
    render(
      <ProtectedRoute>
        <div>Conteúdo protegido</div>
      </ProtectedRoute>,
    );
    expect(replaceMock).toHaveBeenCalledWith("/login");
  });

  it("renderiza os filhos quando o usuário está autenticado e autorizado", () => {
    useAuthMock.mockReturnValue({
      user: { id: "1", name: "Ana", email: "a@a.com", roles: ["STUDENT"] },
      isLoading: false,
      hasRole: (role: string) => role === "STUDENT",
    });
    render(
      <ProtectedRoute allowedRoles={["STUDENT"]}>
        <div>Conteúdo protegido</div>
      </ProtectedRoute>,
    );
    expect(screen.getByText("Conteúdo protegido")).toBeInTheDocument();
    expect(replaceMock).not.toHaveBeenCalled();
  });

  it("redireciona para /dashboard quando o papel não é permitido", () => {
    useAuthMock.mockReturnValue({
      user: { id: "1", name: "Ana", email: "a@a.com", roles: ["STUDENT"] },
      isLoading: false,
      hasRole: () => false,
    });
    render(
      <ProtectedRoute allowedRoles={["SUPER_ADMIN"]}>
        <div>Conteúdo protegido</div>
      </ProtectedRoute>,
    );
    expect(replaceMock).toHaveBeenCalledWith("/dashboard");
  });
});
