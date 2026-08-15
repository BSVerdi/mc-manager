import { useState } from "react";

import Console from "../features/console";
import ServerInfo from "../features/server-info";
import FileViewer from "../features/file-viewer";
import SideMenu from "../features/side-menu";
import PlayersList from "../features/players-list";
import Settings from "../features/settings";

export default function Home() {
  const [selected, setSelected] = useState("Console");

  return (
    <>
      <main>
        <aside className="side-menu">
          <SideMenu selected={selected} setSelected={setSelected} />
        </aside>
        <section className="content">
          <section className="server-info">
            <ServerInfo />
          </section>
          <section className="server-dashboard">
            {(() => {
              switch (selected) {
                case "Console":
                  return <Console />;
                case "Files":
                  return <FileViewer />;
                case "Players":
                  return <PlayersList />;
                case "Settings":
                  return <Settings />;
                default:
                  return <Console />;
              }
            })()}
          </section>
        </section>
      </main>
    </>
  );
}
