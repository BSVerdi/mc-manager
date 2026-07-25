import { useMetricsSocket } from "../../hooks/useMetricsSocket";
import { useStatusSocket } from "../../hooks/useStatusSocket";
import { api } from "../../lib/apiClient";

function formatUptime(totalSeconds: number): string {
  if (totalSeconds <= 0) return "--";

  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = Math.floor(totalSeconds % 60);

  if (hours > 0) return `${hours}h ${minutes}m`;
  if (minutes > 0) return `${minutes}m ${seconds}s`;
  return `${seconds}s`;
}

export default function ServerInfo() {
  const status = useStatusSocket();
  const metrics = useMetricsSocket();

  const handleStartServer = () => {
    api.server.start();
  };

  const handleStopServer = () => {
    api.server.stop();
  };

  const handleRestartServer = () => {
    api.server.restart();
  };

  return (
    <>
      <section className="metrics">
        <ul>
          <li>
            <strong>Server status: </strong>
            <span
              className={
                status === "ONLINE" || status === "STARTING" ? "on" : "off"
              }
            >
              {status.charAt(0) + status.slice(1).toLowerCase()}
            </span>
          </li>
          <li>
            <strong>Uso CPU: </strong>
            {metrics.cpuUsagePercent}%
          </li>
          <li>
            <strong>Uso RAM: </strong>
            {metrics.ramUsedMb}MB / {metrics.ramMaxMb}MB
          </li>
          <li>
            <strong>Server uptime: </strong>
            {formatUptime(metrics.uptimeSeconds)}
          </li>
          <li>
            <strong>Online players: </strong> {metrics.onlinePlayers}
          </li>
        </ul>
      </section>
      <section className="commands-buttons">
        <button onClick={handleStartServer} className="start">
          Iniciar
        </button>
        <button onClick={handleRestartServer} className="restart">
          restart
        </button>
        <button onClick={handleStopServer} className="stop">
          Parar
        </button>
      </section>
    </>
  );
}
