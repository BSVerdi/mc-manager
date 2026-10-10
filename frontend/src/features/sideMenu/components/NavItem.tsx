import { motion } from "framer-motion";
import { NavLink } from "react-router-dom";
import { elevateIn } from "../../../lib/animations/elevateIn";
import type { MenuItem } from "../../../types/menu";

interface SideMenuItemProps {
  item: MenuItem;
  opened?: boolean;
}

export function NavItem({ item, opened = true }: SideMenuItemProps) {
  return (
    <motion.div layout className="nav-item-container">
      <NavLink key={item.id} to={item.path!} className={({ isActive }) => (isActive ? "nav-btn active" : "nav-btn")}>
        <motion.div className="nav-icon-container">{item.icon ? <item.icon /> : null}</motion.div>
        {opened && (
          <motion.span variants={elevateIn} initial="hidden" animate="visible" className="nav-item-label">
            {item.label}
          </motion.span>
        )}
      </NavLink>
    </motion.div>
  );
}
