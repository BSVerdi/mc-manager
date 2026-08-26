import { PlusCircle, Server as ServerIcon } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import type { Server, Server as server } from "../types/Server";
import { api } from "../lib/apiClient";
import { useEffect, useState } from "react";
import { NavLink } from "react-router-dom";
import { useServerList } from "../hooks/useServerList";
import { useServerStatus } from "../hooks/useServerStatus";
import { ServerCard } from "../components/ServerCard";
import CreateServer from "../features/create_server/CreateServer";

export default function Home() {
  const { user } = useAuth();
  const { servers } = useServerList();
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
          <CreateServer isOpen={isOpen} setIsOpen={setIsOpen} />
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
