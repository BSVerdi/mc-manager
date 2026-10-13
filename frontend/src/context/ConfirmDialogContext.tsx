import { AnimatePresence } from "framer-motion";
import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from "react";
import { ConfirmDialog } from "../components/ConfirmDialog";
import type { ConfirmInput } from "../types/confirm";

interface ConfirmDialogContextValue {
  confirm: (content: ConfirmInput) => Promise<boolean>;
}

const ConfirmDialogContext = createContext<ConfirmDialogContextValue | null>(null);

export function ConfirmDialogProvider({ children }: { children: ReactNode }) {
  const [input, setInput] = useState<ConfirmInput | null>(null);
  const resolveRef = useRef<(confirmed: boolean) => void | null>(null);

  const closeDialog = useCallback((confirmed: boolean) => {
    setInput(null);

    if (resolveRef.current) {
      resolveRef.current(confirmed);
      resolveRef.current = null;
    }
  }, []);

  const confirm = useCallback(
    (content: ConfirmInput) =>
      new Promise<boolean>((resolve) => {
        resolveRef.current = resolve;
        setInput(content);
      }),
    [],
  );

  const value = useMemo(() => ({ confirm }), [confirm]);

  return (
    <ConfirmDialogContext.Provider value={value}>
      {children}
      <AnimatePresence initial={false}>
        {input && <ConfirmDialog input={input} onClose={closeDialog} />}
      </AnimatePresence>
    </ConfirmDialogContext.Provider>
  );
}

export function useConfirmDialog() {
  const context = useContext(ConfirmDialogContext);

  if (!context) {
    throw new Error("useConfirmDialog deve ser usado dentro de <ConfirmDialogProvider />");
  }

  return context;
}
