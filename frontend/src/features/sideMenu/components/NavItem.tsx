import { motion } from "framer-motion";
import { useState } from "react";
import { NavLink } from "react-router-dom";
import type { MenuItem } from "../../../types/menu";

interface SideMenuItemProps {
  item: MenuItem;
  opened?: boolean;
}

export const NavItem = ({ item, opened = true }: SideMenuItemProps) => {
  const [isOpen, setIsOpen] = useState<boolean>(false);

  const handleAction = (e: React.MouseEvent) => {
    if (item.children && !item.path) {
      e.preventDefault();
      setIsOpen((prev) => !prev);
    }
  };

  return (
    <motion.div layout className="nav-item-container">
      <NavLink key={item.id} to={item.path!} className={({ isActive }) => (isActive ? "nav-btn active" : "nav-btn")}>
        <motion.div className="nav-icon-container">{item.icon ? <item.icon /> : null}</motion.div>
        {opened && (
          <motion.span
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.125 }}
            className="nav-item-label"
          >
            {item.label}
          </motion.span>
        )}
      </NavLink>
    </motion.div>
  );
};
