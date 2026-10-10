import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { Toaster } from "../components/Toast";
import type { ToastItem, ToastOptions } from "../types/toast";

interface ToastContextValue {
  toast: (message: string, options?: ToastOptions) => string;
  success: (message: string, options?: ToastOptions) => string;
  error: (message: string, options?: ToastOptions) => string;
  info: (message: string, options?: ToastOptions) => string;
  dismiss: (id: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

let counter = 0;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const dismiss = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const toast = useCallback((message: string, options: ToastOptions = {}): string => {
    const id = options.id ?? `toast-${++counter}`;
    const item: ToastItem = {
      id,
      message,
      variant: options.variant ?? "info",
      duration: options.duration ?? 5000,
    };

    setToasts((prev) => (prev.some((t) => t.id === id) ? prev.map((t) => (t.id === id ? item : t)) : [...prev, item]));

    return id;
  }, []);

  const success = useCallback(
    (message: string, options?: ToastOptions) => toast(message, { ...options, variant: "success" }),
    [toast],
  );

  const error = useCallback(
    (message: string, options?: ToastOptions) => toast(message, { ...options, variant: "error" }),
    [toast],
  );

  const info = useCallback(
    (message: string, options?: ToastOptions) => toast(message, { ...options, variant: "info" }),
    [toast],
  );

  const value = useMemo(() => ({ toast, success, error, info, dismiss }), [toast, success, error, info, dismiss]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <Toaster toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);

  if (!context) {
    throw new Error("useToast deve ser usado dentro de <ToastProvider/>");
  }

  return context;
}
