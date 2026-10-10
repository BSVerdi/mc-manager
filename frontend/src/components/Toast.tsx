import { AnimatePresence, motion } from "framer-motion";
import { AlertCircle, CheckCircle2, Info, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import type { ToastItem, ToastVariant } from "../types/toast";
import { Portal } from "./Portal";

interface ToasterProps {
  toasts: ToastItem[];
  onDismiss: (id: string) => void;
}

interface ToastProps {
  item: ToastItem;
  onDismiss: (id: string) => void;
}

const VARIANT_ICONS: Record<ToastVariant, typeof Info> = {
  success: CheckCircle2,
  error: AlertCircle,
  info: Info,
};

function Toast({ item, onDismiss }: ToastProps) {
  const [paused, setPaused] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout>>(undefined);

  useEffect(() => {
    if (paused) return;

    timerRef.current = setTimeout(() => onDismiss(item.id), item.duration);

    return () => clearTimeout(timerRef.current);
  }, [item.id, item.duration, paused, onDismiss]);

  const Icon = VARIANT_ICONS[item.variant];

  return (
    <motion.div
      layout
      initial={{ opacity: 0, y: -16, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -16, scale: 0.95 }}
      transition={{ duration: 0.2 }}
      className={`toast toast-${item.variant}`}
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
    >
      <Icon className="toast-icon" size={18} />
      <span className="toast-message">{item.message}</span>
      <button className="toast-close" onClick={() => onDismiss(item.id)}>
        <X size={14} />
      </button>
    </motion.div>
  );
}

export function Toaster({ toasts, onDismiss }: ToasterProps) {
  return (
    <Portal>
      <div className="toast-container">
        <AnimatePresence initial={false}>
          {toasts.map((t) => (
            <Toast key={t.id} item={t} onDismiss={onDismiss} />
          ))}
        </AnimatePresence>
      </div>
    </Portal>
  );
}
