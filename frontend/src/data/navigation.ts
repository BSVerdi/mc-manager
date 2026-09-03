import {
  Folder,
  LayoutDashboard,
  Server,
  Settings,
  Terminal,
  User,
  type LucideIcon,
} from "lucide-react";

export interface MenuItem {
  id: string;
  icon?: LucideIcon;
  path?: string;
  label: string;
  children?: MenuItem[];
}

export const MENU_ITEMS: MenuItem[] = [
  {
    id: "home",
    icon: LayoutDashboard,
    path: "/",
    label: "Dashboard",
  },
  {
    id: "servers",
    icon: Server,
    label: "My Servers",
  },
  {
    id: "settings",
    icon: Settings,
    path: "settings",
    label: "Settings",
  },
];

export const TOP_MENU_ITEMS: MenuItem[] = [
  {
    id: "console",
    icon: Terminal,
    path: "console",
    label: "Console",
  },
  {
    id: "files",
    icon: Folder,
    path: "files",
    label: "Files",
  },
  {
    id: "players",
    icon: User,
    path: "players",
    label: "Players",
  },
  {
    id: "settings",
    icon: Settings,
    path: "settings",
    label: "Settings",
  },
];
