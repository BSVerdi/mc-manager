import { HardDrive, MonitorCog } from "lucide-react";
import { MENU_ITEMS } from "../../data/navigation";
import { NavItem } from "./NavItem";
import { useServerList } from "../../hooks/useServerList";

export default function SideMenu() {
  const { servers } = useServerList();

  return (
    <>
      <aside className="side-menu">
        <header className="side-menu-header">
          <h1>
            <MonitorCog style={{ width: 48, height: 48 }} />
            MC-Manager
          </h1>
        </header>
        <nav>
          {MENU_ITEMS.map((item) => {
            if (item.id === "servers") {
              item.children = servers.map((server) => ({
                id: `server-${server.id}`,
                icon: HardDrive,
                path: `servers/${server.id}`,
                label: server.name,
              }));

              return <NavItem key={item.id} item={item} />;
            }

            return <NavItem key={item.id} item={item} />;
          })}
        </nav>
        <footer className="side-menu-footer">
          <span>Sistema desenvolvido por Breno Verdi</span>
          <span>Versão Beta 0.1</span>
        </footer>
      </aside>
    </>
  );
}
