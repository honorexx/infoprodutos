import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import CoursesPage from "@/app/courses/page";

const pushMock = vi.fn();
const replaceMock = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock, replace: replaceMock }),
  usePathname: () => "/courses",
}));

const useAuthMock = vi.fn();
vi.mock("@/lib/auth-context", () => ({
  useAuth: () => useAuthMock(),
}));

const apiFetchMock = vi.fn();
vi.mock("@/lib/api-client", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api-client")>("@/lib/api-client");
  return {
    ...actual,
    apiFetch: (...args: unknown[]) => apiFetchMock(...args),
  };
});

const instructorUser = {
  id: "instructor-1",
  name: "Professor Teste",
  email: "prof@example.com",
  roles: ["INSTRUCTOR"],
};

describe("CoursesPage", () => {
  beforeEach(() => {
    pushMock.mockClear();
    replaceMock.mockClear();
    apiFetchMock.mockReset();
    useAuthMock.mockReturnValue({
      user: instructorUser,
      isLoading: false,
      hasRole: (...roles: string[]) => roles.includes("INSTRUCTOR"),
      logout: vi.fn(),
    });
  });

  it("lista os cursos retornados pela API", async () => {
    apiFetchMock.mockResolvedValueOnce({
      content: [
        {
          id: "course-1",
          title: "Curso de Java",
          slug: "curso-de-java",
          coverImageUrl: null,
          priceCents: 0,
          currency: "BRL",
          workloadHours: 10,
          status: "DRAFT",
          createdByName: "Professor Teste",
          createdAt: "2026-01-01T00:00:00Z",
          updatedAt: "2026-01-01T00:00:00Z",
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 50,
      first: true,
      last: true,
    });

    render(<CoursesPage />);

    expect(await screen.findByText("Curso de Java")).toBeInTheDocument();
    expect(screen.getByText("Rascunho")).toBeInTheDocument();
  });

  it("mostra estado vazio quando não há cursos", async () => {
    apiFetchMock.mockResolvedValueOnce({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 50,
      first: true,
      last: true,
    });

    render(<CoursesPage />);

    expect(await screen.findByText("Você ainda não tem nenhum curso.")).toBeInTheDocument();
  });

  it("cria um novo curso e navega para a página de detalhe", async () => {
    apiFetchMock.mockResolvedValueOnce({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 50,
      first: true,
      last: true,
    });
    apiFetchMock.mockResolvedValueOnce({ id: "new-course-id" });

    const user = userEvent.setup();
    render(<CoursesPage />);

    await screen.findByText("Você ainda não tem nenhum curso.");

    await user.click(screen.getByRole("button", { name: /novo curso/i }));
    await user.type(screen.getByLabelText("Título"), "Curso Novo");
    await user.type(screen.getByLabelText(/carga horária/i), "10");
    const price = screen.getByLabelText(/preço/i);
    await user.clear(price);
    await user.type(price, "497");
    await user.click(screen.getByRole("button", { name: /^criar curso$/i }));

    await waitFor(() => expect(pushMock).toHaveBeenCalledWith("/courses/new-course-id"));
    expect(apiFetchMock).toHaveBeenCalledWith(
      "/courses",
      expect.objectContaining({
        method: "POST",
        body: expect.objectContaining({ title: "Curso Novo", workloadHours: 10, priceCents: 49700 }),
      }),
    );
  });
});
