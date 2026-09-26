import { Folder, Settings, Terminal, User } from "lucide-react";
import type { MenuItem } from "../../types/menu";
import { NavItem } from "../sideMenu/components/NavItem";

const NAV_BAR_ITEMS: MenuItem[] = [
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

export default function TopMenu() {
  return (
    <nav className="top-menu-container">
      <div className="top-menu-btns">
        {NAV_BAR_ITEMS.map((item: MenuItem) => (
          <NavItem key={item.id} item={item} />
        ))}
      </div>
    </nav>
  );
}
