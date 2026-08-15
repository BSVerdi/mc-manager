import { useCallback, useEffect, useRef, useState } from "react";
import { onStompConnect, stompClient } from "../lib/stompClient";
import type { Log } from "../types/Log";
import { api } from "../lib/apiClient";

const MAX_LINES = 2000;

export function useConsoleSocket() {
  const [logs, setLogs] = useState<Log[]>([]);

  useEffect(() => {
    let subscription: ReturnType<typeof stompClient.subscribe> | undefined;

    const unregister = onStompConnect(() => {
      subscription = stompClient.subscribe("/topic/console", (message) => {
        try {
          const log: Log = JSON.parse(message.body);

          setLogs((old) => {
            const updated = [...old, log];

            return updated.length > MAX_LINES
              ? updated.slice(-MAX_LINES)
              : updated;
          });
        } catch (err) {
          console.error(err);
        }
      });
    });

    return () => {
      unregister();
      subscription?.unsubscribe();
    };
  }, []);

  const sendCommand = useCallback((command: string) => {
    switch (command.toLocaleLowerCase()) {
      case "start":
        api.server.start();
        break;
      case "clear":
        setLogs([]);
        break;
      default:
        stompClient.publish({
          destination: "/app/command",
          body: command,
        });
    }
  }, []);

  return { logs, sendCommand };
}
