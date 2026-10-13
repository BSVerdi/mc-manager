import { useCallback, useRef, useState } from "react";
import type { ConfirmInput } from "../../../../types/confirm";
import type { ToastOptions } from "../../../../types/toast";

export function useDelete(
  success: (message: string, options: ToastOptions) => void,
  error: (message: string, options: ToastOptions) => void,
  remove: (path: string) => Promise<boolean>,
  confirm: (next: ConfirmInput) => Promise<boolean>,
) {
  const [deleting, setDeleting] = useState<boolean>(false);
  const deletingRef = useRef(false);

  const onDelete = useCallback(
    async (path: string, name: string) => {
      if (deletingRef.current) return;

      const confirmed = await confirm({
        title: `Deletar ${name}`,
        description: `Tem certeza que deseja Deletar ${name}?`,
        confirmLabel: "Deletar",
        cancelLabel: "Cancelar",
      });

      if (!confirmed) return;

      deletingRef.current = true;
      setDeleting(true);

      const ok = await remove(path);

      deletingRef.current = false;
      setDeleting(false);

      if (ok) {
        success(`${name} removido com sucesso.`, {
          id: "browser-remove-success",
          duration: 1000,
        });
      } else {
        error(`Erro ao remover ${name}.`, {
          id: "borwser-remove-error",
          duration: 1000,
        });
      }
    },
    [remove, success, error, confirm],
  );

  return { deleting, onDelete };
}
