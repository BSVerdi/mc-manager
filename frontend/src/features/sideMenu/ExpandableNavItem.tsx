import { AnimatePresence, motion } from "framer-motion";
import { ChevronRight } from "lucide-react";
import { useState } from "react";
import { NavLink } from "react-router-dom";
import type { MenuItem } from "../../data/navigation";

interface ExpandableNavItemProps {
  item: MenuItem;
  isOpen?: boolean;
}

export default function ExpandableNavItemProps({ item, isOpen }: ExpandableNavItemProps) {
  const [isExpanded, setIsExpanded] = useState<boolean>(false);

  const hasChildren = (item.children?.length ?? 0) > 0;

  return (
    <motion.div className="nav-item-wrapper">
      <button
        type="button"
        className="nav-btn chevron"
        onClick={() => hasChildren && setIsExpanded((prev) => !prev)}
        aria-expanded={isExpanded}
        disabled={!hasChildren}
      >
        <motion.div className="nav-icon-container">{item.icon ? <item.icon /> : null}</motion.div>
        {isOpen && (
          <div className="nav-item-submenu">
            <motion.span
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.125 }}
              className="nav-item-label"
            >
              {item.label}
            </motion.span>
            {hasChildren && (
              <motion.div
                initial={false}
                animate={{ rotate: isExpanded ? 90 : 0 }}
                transition={{ duration: 0.1, ease: "easeInOut" }}
              >
                <ChevronRight size={20} />
              </motion.div>
            )}
          </div>
        )}
      </button>

      <AnimatePresence>
        {isOpen && hasChildren && isExpanded && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2, ease: "easeInOut" }}
            className="sub-menu"
          >
            {item.children!.map((child) => (
              <NavLink
                key={child.id}
                to={child.path!}
                className={({ isActive }) => (isActive ? "nav-btn active" : "nav-btn")}
              >
                <motion.div layout className="nav-icon-container">
                  {child.icon ? <child.icon /> : null}
                </motion.div>
                <motion.span
                  layout
                  initial={{ opacity: 0, y: 12 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.125 }}
                  className="nav-item-label"
                >
                  {child.label}
                </motion.span>
              </NavLink>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
