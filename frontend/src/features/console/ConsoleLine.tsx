import type { Log } from "../../types/Log";

type Props = {
  log: Log;
};

export default function ConsoleLine({ log }: Props) {
  const time = new Date(log.timestamp).toLocaleTimeString();
  return (
    <div className="info">
      <span className="timestamp">[{time}]</span>
      <span>{log.message}</span>
    </div>
  );
}
