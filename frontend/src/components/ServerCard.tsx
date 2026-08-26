import { HardDrive, Play, Square } from "lucide-react";
import { useServerControl } from "../hooks/useServerControl";
import { useServerStatus } from "../hooks/useServerStatus";
import type { Server } from "../types/Server";
import { NavLink } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { Status } from "../types/Status";

export function ServerCard({ server }: { server: Server }) {
  const { status, setStatus } = useServerStatus(server.id);
  const { start, stop } = useServerControl(server.id);

  const handleToggleServer = () => {
    if (status === Status.OFFLINE) {
      start();
      setStatus(Status.ONLINE);
    } else {
      stop();
      setStatus(Status.OFFLINE);
    }
  };

  return (
    <div className="server-card">
      <NavLink to={`/servers/${server.id}`} className="server-card-main">
        <HardDrive style={{ width: 64, height: 64, color: "black" }} />
        <div className="server-card-details">
          <h3 className="server-card-title">{server.name}</h3>
          <div className="server-card-info">
            <span>Version: {server.version}</span>
            <span
              className={
                status === Status.ONLINE || status === Status.STARTING
                  ? "on"
                  : "off"
              }
            >
              {status.charAt(0) + status.slice(1).toLowerCase()}
            </span>
          </div>
        </div>
      </NavLink>
      <div className="server-card-btns">
        <button
          onClick={handleToggleServer}
          className={
            status === Status.OFFLINE ? "server-btn start" : "server-btn stop"
          }
        >
          <AnimatePresence mode="wait" initial={false}>
            {status === Status.OFFLINE ? (
              <motion.div
                key="play"
                initial={{ opacity: 0, scale: 0.6 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.6 }}
                transition={{ duration: 0.2 }}
              >
                <Play style={{ width: 32, height: 32 }} />
              </motion.div>
            ) : (
              <motion.div
                key="stop"
                initial={{ opacity: 0, scale: 0.6 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.6 }}
                transition={{ duration: 0.2 }}
              >
                <Square style={{ width: 32, height: 32 }} />
              </motion.div>
            )}
          </AnimatePresence>
        </button>
      </div>
    </div>
  );
}
