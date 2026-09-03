import { AnimatePresence, motion } from "framer-motion";
import { HardDrive, MonitorCog } from "lucide-react";
import { MENU_ITEMS, type MenuItem } from "../../data/navigation";
import { useServerList } from "../../hooks/useServerList";
import { NavItem } from "./NavItem";

interface SideMenuProps {
  opened: boolean;
}

export default function SideMenu({ opened }: SideMenuProps) {
  const { servers } = useServerList();

  const menuItemsWithServers = MENU_ITEMS.map((item) => {
    if (item.id === "servers") {
      return {
        ...item,
        children: servers.map(
          (server): MenuItem => ({
            id: `server-${server.id}`,
            icon: HardDrive,
            path: `servers/${server.id}`,
            label: server.name,
          }),
        ),
      };
    }

    return item;
  });

  return (
    <motion.nav
      layout
      initial={false}
      animate={{ width: opened ? "270px" : "60px" }}
      transition={{ duration: 0.15 }}
      className="side-menu"
    >
      <motion.div layout className="side-menu-header">
        <motion.div layout className="side-menu-icon-container">
          <MonitorCog style={{ width: 48, height: 48 }} />
        </motion.div>
        {opened && (
          <motion.div
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.125 }}
          >
            <h1>MC-Manager</h1>
          </motion.div>
        )}
      </motion.div>
      <div className="side-menu-options">
        {menuItemsWithServers.map((item) => (
          <NavItem item={item} opened={opened} />
        ))}
      </div>
      <footer className="side-menu-footer">
        <AnimatePresence>
          {opened && (
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
    </motion.nav>
  );
}
