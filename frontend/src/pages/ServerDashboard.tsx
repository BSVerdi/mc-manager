import { useState } from "react";
import { Navigate, Outlet, useParams } from "react-router-dom";

export interface DashboardContext {
  serverId: number;
  onlinePlayers: number;
  setOnlinePlayers: (value: number) => void;
}

import TopMenu from "../features/navigation/TopMenu";
import ServerInfo from "../features/server-info/serverInfo";

export default function ServerDashboard() {
  const { serverId } = useParams<{ serverId: string }>();
  const [onlinePlayers, setOnlinePlayers] = useState<number>(0);
  const numericServerId = Number(serverId);

  if (!serverId || Number.isNaN(numericServerId)) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="server-dashboard">
      <ServerInfo
        serverId={numericServerId}
        setOnlinePlayers={setOnlinePlayers}
      />
      <section className="server-content">
        <TopMenu />
        <section className="server-content-pages">
          <Outlet
            context={{
              serverId: numericServerId,
              onlinePlayers: onlinePlayers,
              setOnlinePlayers: setOnlinePlayers,
            }}
          />
        </section>
      </section>
    </div>
  );
}
