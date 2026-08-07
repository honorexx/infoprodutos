import { describe, expect, it } from "vitest";
import { courseFormSchema, lessonFormSchema, loginSchema, moduleFormSchema, registerSchema } from "@/lib/validation";

describe("loginSchema", () => {
  it("aceita e-mail e senha válidos", () => {
    const result = loginSchema.safeParse({ email: "user@example.com", password: "any" });
    expect(result.success).toBe(true);
  });

  it("rejeita e-mail inválido", () => {
    const result = loginSchema.safeParse({ email: "not-an-email", password: "any" });
    expect(result.success).toBe(false);
  });

  it("rejeita senha vazia", () => {
    const result = loginSchema.safeParse({ email: "user@example.com", password: "" });
    expect(result.success).toBe(false);
  });
});

describe("registerSchema", () => {
  const base = {
    name: "Aluno Teste",
    email: "aluno@example.com",
    password: "SenhaForte123",
    confirmPassword: "SenhaForte123",
  };

  it("aceita dados válidos", () => {
    expect(registerSchema.safeParse(base).success).toBe(true);
  });

  it("rejeita senha curta", () => {
    const result = registerSchema.safeParse({ ...base, password: "123", confirmPassword: "123" });
    expect(result.success).toBe(false);
  });

  it("rejeita quando senhas não coincidem", () => {
    const result = registerSchema.safeParse({ ...base, confirmPassword: "Outra123" });
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0].path).toContain("confirmPassword");
    }
  });

  it("rejeita nome muito curto", () => {
    const result = registerSchema.safeParse({ ...base, name: "A" });
    expect(result.success).toBe(false);
  });
});

describe("courseFormSchema", () => {
  it("exige carga horária", () => {
    expect(courseFormSchema.safeParse({ title: "Curso de Java" }).success).toBe(false);
  });

  it("rejeita título vazio", () => {
    expect(courseFormSchema.safeParse({ title: "", workloadHours: "10" }).success).toBe(false);
  });

  it("aceita carga horária numérica válida", () => {
    expect(courseFormSchema.safeParse({ title: "Curso", workloadHours: "10" }).success).toBe(true);
  });

  it("rejeita carga horária negativa", () => {
    const result = courseFormSchema.safeParse({ title: "Curso", workloadHours: "-5" });
    expect(result.success).toBe(false);
  });
});

describe("moduleFormSchema", () => {
  it("aceita título obrigatório", () => {
    expect(moduleFormSchema.safeParse({ title: "Introdução" }).success).toBe(true);
  });

  it("rejeita título vazio", () => {
    expect(moduleFormSchema.safeParse({ title: "" }).success).toBe(false);
  });
});

describe("lessonFormSchema", () => {
  it("aceita dados válidos com FREE_PREVIEW", () => {
    const result = lessonFormSchema.safeParse({ title: "Aula 1", accessType: "FREE_PREVIEW" });
    expect(result.success).toBe(true);
  });

  it("rejeita accessType inválido", () => {
    const result = lessonFormSchema.safeParse({ title: "Aula 1", accessType: "INVALID" });
    expect(result.success).toBe(false);
  });

  it("rejeita duração negativa", () => {
    const result = lessonFormSchema.safeParse({
      title: "Aula 1",
      accessType: "ENROLLED_ONLY",
      durationSeconds: "-10",
    });
    expect(result.success).toBe(false);
  });
});
