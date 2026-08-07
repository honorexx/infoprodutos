/**
 * Ponto de importação nomeado "app-" para o botão do design system, usado
 * pelos componentes novos do dashboard (`config/navigation.ts` em diante).
 * A implementação real vive em `button.tsx` — mantemos um único componente
 * de botão para não divergir estilos; este arquivo só reexporta com o nome
 * pedido na estrutura de pastas do sistema visual.
 */
export { Button as AppButton, buttonVariants as appButtonVariants } from "@/components/ui/button";
