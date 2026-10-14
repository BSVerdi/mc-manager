import { useCallback, useRef, useState } from "react";
import { useConfirmDialog } from "../../../../context/ConfirmDialogContext";
import { useToast } from "../../../../context/ToastContext";

export function useDelete(remove: (path: string) => Promise<boolean>) {
  const [deleting, setDeleting] = useState<boolean>(false);
  const deletingRef = useRef(false);

  const { success, error } = useToast();
  const { confirm } = useConfirmDialog();

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
