import { z } from "zod";

export const loginSchema = z.object({
  email: z.string().min(1, "Informe o e-mail.").email("E-mail inválido."),
  password: z.string().min(1, "Informe a senha."),
});

export type LoginInput = z.infer<typeof loginSchema>;

export const registerSchema = z
  .object({
    name: z.string().min(2, "Nome deve ter ao menos 2 caracteres.").max(150),
    email: z.string().min(1, "Informe o e-mail.").email("E-mail inválido."),
    password: z
      .string()
      .min(8, "A senha deve ter entre 8 e 72 caracteres.")
      .max(72, "A senha deve ter entre 8 e 72 caracteres."),
    confirmPassword: z.string(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "As senhas não coincidem.",
    path: ["confirmPassword"],
  });

export type RegisterInput = z.infer<typeof registerSchema>;

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, "Informe a senha atual."),
    newPassword: z
      .string()
      .min(8, "A senha deve ter entre 8 e 72 caracteres.")
      .max(72, "A senha deve ter entre 8 e 72 caracteres."),
    confirmNewPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmNewPassword, {
    message: "As senhas não coincidem.",
    path: ["confirmNewPassword"],
  });

export type ChangePasswordInput = z.infer<typeof changePasswordSchema>;

const optionalNonNegativeNumberString = z
  .string()
  .optional()
  .refine((value) => !value || (!Number.isNaN(Number(value)) && Number(value) >= 0), {
    message: "Informe um número válido e não negativo.",
  });

const requiredWorkloadHours = z
  .string()
  .min(1, "Informe a carga horária.")
  .refine((value) => !Number.isNaN(Number(value)) && Number(value) >= 0.5, {
    message: "Carga horária mínima: 0,5h.",
  });

export const courseFormSchema = z.object({
  title: z.string().min(1, "Informe o título.").max(200, "Título muito longo."),
  description: z.string().max(4000, "Descrição muito longa.").optional().or(z.literal("")),
  workloadHours: requiredWorkloadHours,
  priceReais: z
    .string()
    .min(1, "Informe o preço.")
    .refine((value) => !Number.isNaN(Number(value)) && Number(value) >= 0, {
      message: "Preço inválido.",
    }),
});

export type CourseFormInput = z.infer<typeof courseFormSchema>;

export const packageFormSchema = z.object({
  title: z.string().min(1, "Informe o título.").max(200),
  description: z.string().max(4000).optional().or(z.literal("")),
  priceReais: z
    .string()
    .min(1, "Informe o preço.")
    .refine((value) => !Number.isNaN(Number(value)) && Number(value) > 0, {
      message: "Preço do pacote deve ser maior que zero.",
    }),
  active: z.boolean(),
  courseIds: z.array(z.string().uuid()).min(1, "Selecione ao menos um curso."),
});

export type PackageFormInput = z.infer<typeof packageFormSchema>;

export const moduleFormSchema = z.object({
  title: z.string().min(1, "Informe o título.").max(200, "Título muito longo."),
  description: z.string().max(4000, "Descrição muito longa.").optional().or(z.literal("")),
});

export type ModuleFormInput = z.infer<typeof moduleFormSchema>;

export const lessonFormSchema = z.object({
  title: z.string().min(1, "Informe o título.").max(200, "Título muito longo."),
  description: z.string().max(4000, "Descrição muito longa.").optional().or(z.literal("")),
  durationSeconds: optionalNonNegativeNumberString,
  accessType: z.enum(["FREE_PREVIEW", "ENROLLED_ONLY"]),
});

export type LessonFormInput = z.infer<typeof lessonFormSchema>;
