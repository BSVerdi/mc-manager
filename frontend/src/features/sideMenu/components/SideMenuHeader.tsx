import { motion } from "framer-motion";
import { MonitorCog } from "lucide-react";
import { elevateIn } from "../../../lib/animations/elevateIn";

interface SideMenuHeaderProps {
  isOpen: boolean;
}

export function SideMenuHeader({ isOpen }: SideMenuHeaderProps) {
  return (
    <motion.div layout className="side-menu-header">
      <motion.div layout className="side-menu-icon-container">
        <MonitorCog style={{ width: 48, height: 48 }} />
      </motion.div>
      {isOpen && (
        <motion.div variants={elevateIn} initial="hidden" animate="visible">
          <h1>MC-Manager</h1>
        </motion.div>
      )}
    </motion.div>
  );
}
