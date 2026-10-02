import { AnimatePresence } from "framer-motion";
import { useCallback, useRef, useState } from "react";
import { ConfirmDialog } from "../components/ConfirmDialog";
import type { ConfirmInput } from "../types/confirm";

export function useConfirm() {
  const [input, setInput] = useState<ConfirmInput | null>(null);
  const resolverRef = useRef<((confirmed: boolean) => void) | null>(null);

  const closeDialog = useCallback((confimed: boolean) => {
    setInput(null);

    if (resolverRef.current) {
      resolverRef.current(confimed);
      resolverRef.current = null;
    }
  }, []);

  const confirm = useCallback(
    (next: ConfirmInput) =>
      new Promise<boolean>((resolve) => {
        resolverRef.current = resolve;
        setInput(next);
      }),
    [],
  );

  const modal = (
    <AnimatePresence initial={false}>{input && <ConfirmDialog input={input} onClose={closeDialog} />}</AnimatePresence>
  );

  return { confirm, modal };
}
