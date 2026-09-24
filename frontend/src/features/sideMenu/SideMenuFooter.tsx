import { AnimatePresence, motion } from "framer-motion";

interface SideMenuFooterProps {
  isOpen: boolean;
}

export default function SideMenuFooter({ isOpen }: SideMenuFooterProps) {
  return (
    <footer className="side-menu-footer">
      <AnimatePresence>
        {isOpen && (
          <motion.span
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.125 }}
            style={{ overflow: "hidden", whiteSpace: "nowrap" }}
          >
            Sistema desenvolvido por Breno Verdi
          </motion.span>
        )}
      </AnimatePresence>
      <span>V1.0</span>
    </footer>
  );
}
