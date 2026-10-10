import { motion } from "framer-motion";
import { TriangleAlert } from "lucide-react";
import { elevateIn } from "../lib/animations/elevateIn";
import type { ConfirmInput } from "../types/confirm";
import { Portal } from "./Portal";

interface ConfirmDialogProps {
  input: ConfirmInput;
  onClose: (confirmed: boolean) => void;
}

export function ConfirmDialog({ input, onClose }: ConfirmDialogProps) {
  return (
    <Portal>
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.2 }}
        className="confirm-dialog-container"
      >
        <motion.div
          className="confirm-dialog"
          initial={{ opacity: 0, scale: 0.7 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.7 }}
          transition={{ duration: 0.2 }}
        >
          <motion.div>
            <TriangleAlert className="confirm-dialog-warning" size={48} color="#ef4444" />
          </motion.div>
          <motion.h2 variants={elevateIn} initial="hidden" animate="visible" className="confirm-dialog-title">
            {input.title}
          </motion.h2>
          <motion.span variants={elevateIn} initial="hidden" animate="visible" className="confirm-dialog-description">
            {input.description} <br />
            Essa ação não pode ser desfeita.
          </motion.span>
          <div className="confirm-dialog-btns">
            <motion.button className="confirm-dialog-btn" onClick={() => onClose(false)}>
              {input.cancelLabel}
            </motion.button>
            <motion.button className="confirm-dialog-btn confirm" onClick={() => onClose(true)}>
              {input.confirmLabel}
            </motion.button>
          </div>
        </motion.div>
      </motion.div>
    </Portal>
  );
}
