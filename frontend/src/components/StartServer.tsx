import { AnimatePresence, motion } from "framer-motion";
import { Play, Square } from "lucide-react";
import { Status } from "../types/status";

type StartServerProps = {
  loading: boolean;
  status: Status;
  onStart: () => void;
  onStop: () => void;
};

export function StartServer({ loading, status, onStart, onStop }: StartServerProps) {
  const isStart = status === Status.OFFLINE || status === Status.STOPPING;

  const handleClick = () => {
    if (loading) return;

    isStart ? onStart() : onStop();
  };

  return (
    <button onClick={handleClick} disabled={loading} className={isStart ? "server-btn start" : "server-btn stop"}>
      <AnimatePresence mode="wait" initial={false}>
        {isStart ? (
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
