import { AnimatePresence, motion } from "framer-motion";
import { ChevronRight } from "lucide-react";
import { useState } from "react";
import { NavLink } from "react-router-dom";
import type { MenuItem } from "../../data/navigation";

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

  if (item.path) {
    return (
      <motion.div layout className="nav-item-container">
        <NavLink
          key={item.id}
          to={item.path}
          className={({ isActive }) =>
            isActive ? "nav-btn active" : "nav-btn"
          }
        >
          <motion.div className="nav-icon-container">
            {item.icon ? <item.icon /> : null}
          </motion.div>
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
  }

  return (
    <motion.div className="nav-item-wrapper">
      <div className="nav-btn chevron" onClick={handleAction}>
        <motion.div className="nav-icon-container">
          {item.icon ? <item.icon /> : null}
        </motion.div>
        {opened && (
          <div className="nav-item-submenu">
            <motion.span
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.125 }}
              className="nav-item-label"
            >
              {item.label}
            </motion.span>
            <motion.div
              initial={false}
              animate={{ rotate: isOpen ? 90 : 0 }}
              transition={{ duration: 0.1, ease: "easeInOut" }}
            >
              <ChevronRight size={20} />
            </motion.div>
          </div>
        )}
      </div>

      <AnimatePresence>
        {opened && item.children && isOpen && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2, ease: "easeInOut" }}
            className="sub-menu"
          >
            {item.children.map((child) => (
              <NavLink
                key={child.id}
                to={child.path!}
                className={({ isActive }) =>
                  isActive ? "nav-btn active" : "nav-btn"
                }
              >
                <motion.div layout className="nav-icon-container">
                  {child.icon ? <child.icon /> : null}
                </motion.div>
                <motion.div>
                  {opened && (
                    <motion.span
                      layout
                      initial={{ opacity: 0, y: 12 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.125 }}
                      className="nav-item-label"
                    >
                      {child.label}
                    </motion.span>
                  )}
                </motion.div>
              </NavLink>
            ))}
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );

  // return item.path ? (
  //   <NavLink
  //     to={item.path}
  //     className={({ isActive }) => (isActive ? "btn active" : "btn")}
  //   >
  //     {item.icon ? <item.icon /> : null}
  //     {item.label}
  //   </NavLink>
  // ) : (
  //   <>
  //     <div className="btn chevron" onClick={handleAction}>
  //       <span>
  //         {item.icon ? <item.icon /> : null}
  //         {item.label}
  //       </span>
  //       <motion.div
  //         key={isOpen ? "down" : "right"}
  //         animate={{ rotate: isOpen ? 90 : 0 }}
  //         transition={{ duration: 0.6, ease: "easeInOut" }}
  //         initial={false}
  //       >
  //         <ChevronRight size={20} />
  //       </motion.div>
  //     </div>
  //     <AnimatePresence>
  //       {item.children && isOpen && (
  //         <motion.div
  //           initial={{ height: 0, opacity: 0 }}
  //           animate={{ height: "auto", opacity: 1 }}
  //           exit={{ height: 0, opacity: 0 }}
  //           transition={{ duration: 0.4, ease: "easeInOut" }}
  //           className="sub-menu"
  //         >
  //           {item.children.map((child) => (
  //             <NavLink
  //               key={child.id}
  //               to={child.path!}
  //               className={({ isActive }) =>
  //                 isActive ? "sub-item active" : "sub-item"
  //               }
  //             >
  //               {child.icon ? <child.icon /> : null}
  //               {child.label}
  //             </NavLink>
  //           ))}
  //         </motion.div>
  //       )}
  //     </AnimatePresence>
  //   </>
  // );
};
