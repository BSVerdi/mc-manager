import type { Log } from "../types";
import { parseTime } from "../utils/parseTime";

interface ConsoleLineProps {
  log: Log;
}

export function ConsoleLine({ log }: ConsoleLineProps) {
  const time = parseTime(log.timestamp);

  return (
    <div className="info">
      <span className="timestamp">[{time}]</span>
      <span>{log.message}</span>
    </div>
  );
}
