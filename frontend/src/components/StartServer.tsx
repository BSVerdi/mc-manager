import { AnimatePresence, motion } from "framer-motion";
import { useServerControl } from "../hooks/useServerControl";
import { Status } from "../types/Status";
import { Play, Square } from "lucide-react";

type StartServerProps = {
  serverId: number;
  status: Status;
  setStatus?: (status: Status) => void;
};

export function StartServer({ serverId, status, setStatus }: StartServerProps) {
  const { start, stop } = useServerControl(serverId);

  const handleToggleServer = () => {
    if (status === Status.OFFLINE) {
      start();

      if (setStatus) setStatus(Status.ONLINE);
    } else {
      stop();

      if (setStatus) setStatus(Status.OFFLINE);
    }
  };

  return (
    <button
      onClick={handleToggleServer}
      className={
        status === Status.OFFLINE || status === Status.STOPPING
          ? "server-btn start"
          : "server-btn stop"
      }
    >
      <AnimatePresence mode="wait" initial={false}>
        {status === Status.OFFLINE || status === Status.STOPPING ? (
          <motion.div
            key="play"
            initial={{ opacity: 0, scale: 0.6 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.6, rotate: 90 }}
            transition={{ duration: 0.2 }}
          >
            <Play style={{ width: 32, height: 32 }} />
          </motion.div>
        ) : (
          <motion.div
            key="stop"
            initial={{ opacity: 0, scale: 0.6 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.6, rotate: 90 }}
            transition={{ duration: 0.2 }}
          >
            <Square style={{ width: 32, height: 32 }} />
          </motion.div>
        )}
      </AnimatePresence>
    </button>
  );
}
