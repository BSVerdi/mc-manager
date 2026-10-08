import { motion } from "framer-motion";
import { useServerList } from "../../hooks/useServerList";
import { ExpandableNavItem } from "./components/ExpandableNavItem";
import { NavItem } from "./components/NavItem";
import { SideMenuFooter } from "./components/SideMenuFooter";
import { SideMenuHeader } from "./components/SideMenuHeader";
import { useMenuItems } from "./hooks/useMenuItems";

interface SideMenuProps {
  opened: boolean;
}

export function SideMenu({ opened }: SideMenuProps) {
  const { servers } = useServerList();
  const menuItems = useMenuItems(servers);

  return (
    <motion.nav
      layout
      initial={false}
      animate={{ width: opened ? "300px" : "60px" }}
      transition={{ duration: 0.15 }}
      className="side-menu"
    >
      <SideMenuHeader isOpen={opened} />
      <div className="side-menu-options">
        {menuItems.map((item) =>
          item.children ? (
            <ExpandableNavItem key={item.id} item={item} isOpen={opened} />
          ) : (
            <NavItem key={item.id} item={item} opened={opened} />
          ),
        )}
      </div>
      <SideMenuFooter isOpen={opened} />
    </motion.nav>
  );
}
