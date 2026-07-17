import { useState } from "react";
// import "./style.css";

import Console from "../../features/console";
import ServerInfo from "../../features/server-info";
import { Folder, MonitorCog } from "lucide-react";
import { Terminal } from "lucide-react";
import FileViewer from "../../features/file-viewer";

export default function Home() {
  const [selected, setSelected] = useState("Console");

  return (
    <>
      <main>
        <aside className="side-menu">
          <h1>
            <MonitorCog style={{ width: 48, height: 48 }} />
            MC-Manager
          </h1>
          <nav>
            <button
              onClick={() => {
                if (selected !== "Console") {
                  setSelected("Console");
                }
              }}
              className={selected === "Console" ? "active" : ""}
            >
              <Terminal style={{ width: 20, height: 20 }} />
              Terminal
            </button>
            <button
              onClick={() => {
                if (selected !== "Files") {
                  setSelected("Files");
                }
              }}
              className={selected === "Files" ? "active" : ""}
            >
              <Folder style={{ width: 20, height: 20 }} />
              Arquivos
            </button>
          </nav>
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
