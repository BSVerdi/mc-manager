import { motion } from "framer-motion";
import { MonitorCog } from "lucide-react";

interface SideMenuHeaderProps {
  isOpen: boolean;
}

export default function ({ isOpen }: SideMenuHeaderProps) {
  return (
    <motion.div layout className="side-menu-header">
      <motion.div layout className="side-menu-icon-container">
        <MonitorCog style={{ width: 48, height: 48 }} />
      </motion.div>
      {isOpen && (
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.125 }}>
          <h1>MC-Manager</h1>
        </motion.div>
      )}
    </motion.div>
  );
}
