import { HardDrive } from "lucide-react";
import { NavLink } from "react-router-dom";
import { StartServer } from "../../../components/StartServer";
import { useServerControl } from "../../../hooks/useServerControl";
import { useServerStatus } from "../../../hooks/useServerStatus";
import type { Server } from "../../../types/server";
import { Status } from "../../../types/status";

export function ServerCard({ server }: { server: Server }) {
  const { status } = useServerStatus(server.id);
  const { loading, start, stop } = useServerControl(server.id);

  return (
    <div className="server-card">
      <NavLink to={`/servers/${server.id}`} className="server-card-main">
        <HardDrive style={{ width: 64, height: 64, color: "black" }} className="server-card-icon" />
        <div className="server-card-details">
          <h3 className="server-card-title">{server.name}</h3>
          <div className="server-card-info">
            <span>Version: {server.version}</span>
            <span className={status === Status.ONLINE || status === Status.STARTING ? "on" : "off"}>
              {status.charAt(0) + status.slice(1).toLowerCase()}
            </span>
          </div>
        </div>
      </NavLink>
      <div className="server-card-btns">
        <StartServer loading={loading} onStart={start} onStop={stop} status={status} />
      </div>
    </div>
  );
}
