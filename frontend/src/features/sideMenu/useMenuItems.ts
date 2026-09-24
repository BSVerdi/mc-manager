import { HardDrive, LayoutDashboard, Server as ServerIcon, Settings } from "lucide-react";
import { useMemo } from "react";
import type { MenuItem } from "../../types/menu";
import type { Server } from "../../types/server";

const MENU_ITEMS: MenuItem[] = [
  {
    id: "home",
    icon: LayoutDashboard,
    path: "/",
    label: "Dashboard",
  },
  {
    id: "servers",
    icon: ServerIcon,
    label: "My Servers",
  },
  {
    id: "settings",
    icon: Settings,
    path: "settings",
    label: "Settings",
  },
];

export function useMenuItems(servers: Server[]): MenuItem[] {
  return useMemo(() => {
    return MENU_ITEMS.map((item) => {
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
  }, [servers]);
}
