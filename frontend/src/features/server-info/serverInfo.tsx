import { AnimatePresence, motion } from "framer-motion";
import { StartServer } from "../../components/StartServer";
import { useMetricsSocket } from "../../hooks/useMetricsSocket";
import { useServerControl } from "../../hooks/useServerControl";
import { useServerData } from "../../hooks/useServerData";
import { useStatusSocket } from "../../hooks/useStatusSocket";
import { api } from "../../lib/apiClient";
import { Status } from "../../types/Status";
import { RotateCcw } from "lucide-react";

function formatUptime(totalSeconds: number): string {
  if (totalSeconds <= 0) return "--";

  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = Math.floor(totalSeconds % 60);

  if (hours > 0) return `${hours}h ${minutes}m`;
  if (minutes > 0) return `${minutes}m ${seconds}s`;
  return `${seconds}s`;
}

export default function ServerInfo({ serverId }: { serverId: number }) {
  const status = useStatusSocket(serverId);
  const metrics = useMetricsSocket(serverId);
  const { server } = useServerData(serverId);
  const { restart } = useServerControl(serverId);

  return (
    <section className="server-info">
      <section className="metrics">
        <ul>
          <li>
            <strong>Server Name: </strong>
            <span>{server?.name}</span>
          </li>
          <li>
            <strong>Server Status: </strong>
            <span
              className={
                status === Status.ONLINE || status === Status.STARTING
                  ? "on"
                  : "off"
              }
            >
              {status.charAt(0) + status.slice(1).toLowerCase()}
            </span>
          </li>
          <li>
            <strong>CPU Usage: </strong>
            {status === Status.OFFLINE ? 0 : metrics.cpuUsagePercent}%
          </li>
          <li>
            <strong>RAM Usage: </strong>
            {status === Status.OFFLINE
              ? "0MB / 0MB"
              : `${metrics.ramUsedMb}MB / ${metrics.ramMaxMb}MB`}
          </li>
          <li>
            <strong>Server Uptime: </strong>
            {status === Status.OFFLINE
              ? "--"
              : formatUptime(metrics.uptimeSeconds)}
          </li>
          <li>
            <strong>Server Version: </strong>
            <span>{server?.version}</span>
          </li>
          <li>
            <strong>Server Port: </strong>
            <span>{server?.port}</span>
          </li>
          <li>
            <strong>Online players: </strong> {metrics.onlinePlayers}
          </li>
        </ul>
      </section>
      <section className="commands-buttons">
        <StartServer serverId={serverId} status={status} />
        <button key="restart" onClick={restart} className="restart-btn">
          <RotateCcw style={{ width: 32, height: 32 }} />
        </button>
      </section>
    </section>
  );
}
