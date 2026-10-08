import { AnimatePresence, motion } from "framer-motion";

interface SubmitButtonProps {
  type?: "button" | "submit" | "reset";
  label: string;
  loading: boolean;
  onClick?: () => void;
  disabled?: boolean;
}

export function SubmitButton({ type = "button", label, loading, onClick, disabled }: SubmitButtonProps) {
  const isDisabled = loading || disabled;

  return (
    <motion.button
      type={type}
      className="submit-btn"
      disabled={isDisabled}
      aria-busy={loading}
      onClick={type === "button" && !isDisabled ? onClick : undefined}
      animate={{
        backgroundColor: loading ? "#bfdbfe" : "#2563eb",
        scale: loading ? 0.97 : 1,
      }}
      transition={{ duration: 0.2, ease: "easeInOut" }}
      whileHover={!isDisabled ? { y: -2 } : undefined}
      whileTap={!isDisabled ? { scale: 0.96 } : undefined}
    >
      <AnimatePresence mode="wait" initial={false}>
        {loading ? (
          <motion.span
            key="spinner"
            className="submit-spinner-container"
            role="status"
            aria-label="Processando"
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
          >
            <span className="submit-spinner" />
          </motion.span>
        ) : (
          <motion.span
            key="label"
            className="submit-btn-label"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.125, ease: "easeIn" }}
          >
            {label}
          </motion.span>
        )}
      </AnimatePresence>
    </motion.button>
  );
}
