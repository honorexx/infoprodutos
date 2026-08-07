import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import LoginPage from "@/app/login/page";
import { ApiError } from "@/lib/api-client";

const pushMock = vi.fn();
const loginMock = vi.fn();
vi.mock("@/lib/auth-context", () => ({
  useAuth: () => ({ login: loginMock, user: null, isLoading: false }),
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock, replace: pushMock }),
}));

describe("LoginPage", () => {
  beforeEach(() => {
    pushMock.mockClear();
    loginMock.mockReset();
  });

  it("exibe erros de validação para campos inválidos", async () => {
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.click(screen.getByRole("button", { name: /entrar/i }));

    expect(await screen.findByText("Informe o e-mail.")).toBeInTheDocument();
    expect(loginMock).not.toHaveBeenCalled();
  });

  it("chama login e redireciona para /dashboard em caso de sucesso", async () => {
    loginMock.mockResolvedValue(undefined);
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.type(screen.getByLabelText(/e-mail/i), "user@example.com");
    await user.type(screen.getByLabelText(/senha/i), "SenhaForte123");
    await user.click(screen.getByRole("button", { name: /entrar/i }));

    await waitFor(() => expect(loginMock).toHaveBeenCalledWith("user@example.com", "SenhaForte123"));
    await waitFor(() => expect(pushMock).toHaveBeenCalledWith("/dashboard"));
  });

  it("mostra mensagem de erro do servidor em credenciais inválidas", async () => {
    loginMock.mockRejectedValue(
      new ApiError(
        401,
        {
          type: "invalid-credentials",
          title: "Credenciais inválidas",
          status: 401,
          detail: "E-mail ou senha incorretos.",
          instance: "/api/v1/auth/login",
          timestamp: "2026-01-01T00:00:00Z",
          correlationId: null,
          errors: [],
        },
        "E-mail ou senha incorretos.",
      ),
    );
    const user = userEvent.setup();
    render(<LoginPage />);

    await user.type(screen.getByLabelText(/e-mail/i), "user@example.com");
    await user.type(screen.getByLabelText(/senha/i), "senhaErrada");
    await user.click(screen.getByRole("button", { name: /entrar/i }));

    expect(await screen.findByText("E-mail ou senha incorretos.")).toBeInTheDocument();
  });
});
