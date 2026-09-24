import { motion } from "framer-motion";
import { useServerList } from "../../hooks/useServerList";
import ExpandableNavItem from "./ExpandableNavItem";
import { NavItem } from "./NavItem";
import SideMenuFooter from "./SideMenuFooter";
import SideMenuHeader from "./SideMenuHeader";
import { useMenuItems } from "./useMenuItems";

interface SideMenuProps {
  opened: boolean;
}

export default function SideMenu({ opened }: SideMenuProps) {
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
