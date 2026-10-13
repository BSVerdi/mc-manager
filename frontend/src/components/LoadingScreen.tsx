import { AnimatePresence, motion } from "framer-motion";

export function LoadingScreen() {
  return (
    <AnimatePresence>
      <motion.div
        className="loading-overlay"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.5 }}
      >
        <div className="spinner" />
      </motion.div>
    </AnimatePresence>
  );
}
