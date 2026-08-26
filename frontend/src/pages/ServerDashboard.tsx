import ServerInfo from "../features/server-info/serverInfo";
import TopMenu from "../features/navigation/TopMenu";
import { Navigate, Outlet, useParams } from "react-router-dom";
import { useState } from "react";
import Console from "./Console";
import { useServerId } from "../hooks/useServerId";

export default function ServerDashboard() {
  const { serverId } = useParams<{ serverId: string }>();
  const numericServerId = Number(serverId);

  if (!serverId || Number.isNaN(numericServerId)) {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="server-dashboard">
      <ServerInfo serverId={numericServerId} />
      <section className="server-content">
        <TopMenu />
        <section className="server-content-pages">
          <Outlet context={{ serverId: numericServerId }} />
        </section>
      </section>
    </div>
  );
}
