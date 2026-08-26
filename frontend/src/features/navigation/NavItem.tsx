import { useState } from "react";
import type { MenuItem } from "../../data/navigation";
import { NavLink } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { ChevronRight } from "lucide-react";

interface SideMenuItemProps {
  item: MenuItem;
}

export const NavItem = ({ item }: SideMenuItemProps) => {
  const [isOpen, setIsOpen] = useState<boolean>(false);

  const handleAction = (e: React.MouseEvent) => {
    if (item.children && !item.path) {
      e.preventDefault();
      setIsOpen(!isOpen);
    }
  };

  return item.path ? (
    <NavLink
      to={item.path}
      className={({ isActive }) => (isActive ? "btn active" : "btn")}
    >
      {item.icon ? <item.icon /> : null}
      {item.label}
    </NavLink>
  ) : (
    <>
      <div className="btn chevron" onClick={handleAction}>
        <span>
          {item.icon ? <item.icon /> : null}
          {item.label}
        </span>
        <motion.div
          key={isOpen ? "down" : "right"}
          animate={{ rotate: isOpen ? 90 : 0 }}
          transition={{ duration: 0.6, ease: "easeInOut" }}
          initial={false}
        >
          <ChevronRight size={20} />
        </motion.div>
      </div>
      <AnimatePresence>
        {item.children && isOpen && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.4, ease: "easeInOut" }}
            className="sub-menu"
          >
            {item.children.map((child) => (
              <NavLink
                key={child.id}
                to={child.path!}
                className={({ isActive }) =>
                  isActive ? "sub-item active" : "sub-item"
                }
              >
                {child.icon ? <child.icon /> : null}
                {child.label}
              </NavLink>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};
