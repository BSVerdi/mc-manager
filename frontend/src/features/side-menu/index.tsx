import { Folder, MonitorCog, Settings, Terminal, User } from "lucide-react";

type Props = {
  selected: string;
  setSelected: React.Dispatch<React.SetStateAction<string>>;
};

export default function SideMenu({ selected, setSelected }: Props) {
  return (
    <>
      <header className="side-menu-header">
        <h1>
          <MonitorCog style={{ width: 48, height: 48 }} />
          MC-Manager
        </h1>
      </header>
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
        <button
          onClick={() => {
            if (selected !== "Players") {
              setSelected("Players");
            }
          }}
          className={selected === "Players" ? "active" : ""}
        >
          <User style={{ width: 20, height: 20 }} />
          Players
        </button>
        <button
          onClick={() => {
            if (selected !== "Settings") {
              setSelected("Settings");
            }
          }}
          className={selected === "Settings" ? "active" : ""}
        >
          <Settings style={{ width: 20, height: 20 }} />
          Settings
        </button>
      </nav>
      <footer className="side-menu-footer">
        <span>Sistema desenvolvido por Breno Verdi</span>
        <span>Versão Beta 0.1</span>
      </footer>
    </>
  );
}
