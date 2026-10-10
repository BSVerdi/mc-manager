import { PlusCircle, Server as ServerIcon } from "lucide-react";
import { useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { useServers } from "../../context/ServersContext";
import CreateServer from "./components/CreateServer";
import { ServerCard } from "./components/ServerCard";

export function Home() {
  const { user } = useAuth();
  const { servers, reload } = useServers();
  const [isOpen, setIsOpen] = useState<boolean>(false);

  return (
    <div className="user-dashboard">
      <section className="user-dashboard-header">
        <h2>Dashboard</h2>
      </section>
      <section className="infos">
        <h3>Bem vindo de volta, {user?.username}!</h3>
      </section>
      <section className="server-list-container">
        <section className="server-list-header">
          <span>
            <ServerIcon /> All servers
          </span>
          <button onClick={() => setIsOpen(true)} className="create-server-btn">
            <PlusCircle /> Create New Server
          </button>
          <CreateServer isOpen={isOpen} setIsOpen={setIsOpen} onCreate={reload} />
        </section>
        <section className="server-grid-container">
          <section className="server-grid">
            {servers.map((server) => (
              <ServerCard key={server.id} server={server} />
            ))}
          </section>
        </section>
      </section>
    </div>
  );
}
