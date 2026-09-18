import { useCallback, useEffect, useState } from "react";
import { api } from "../lib/apiClient";
import {
  destinations,
  onStompConnect,
  stompClient,
  topics,
} from "../lib/stompClient";
import type { Log } from "../types/log";

const MAX_LINES = 2000;

export function useConsoleSocket(serverId: number) {
  const [logs, setLogs] = useState<Log[]>([]);

  useEffect(() => {
    setLogs([]);

    let subscription: ReturnType<typeof stompClient.subscribe> | undefined;

    const unregister = onStompConnect(() => {
      subscription = stompClient.subscribe(
        topics.console(serverId),
        (message) => {
          try {
            const log: Log = JSON.parse(message.body);

            setLogs((old) => {
              const updated = [...old, log];

              return updated.length > MAX_LINES
                ? updated.slice(-MAX_LINES)
                : updated;
            });
          } catch (err) {
            console.error("[useConsoleSocket] JSON inválido: ", err);
          }
        },
      );
    });

    return () => {
      unregister();
      subscription?.unsubscribe();
    };
  }, [serverId]);

  const sendCommand = useCallback(
    (command: string) => {
      switch (command.toLocaleLowerCase()) {
        case "start":
          api.server.start(serverId);
          break;
        case "clear":
          setLogs([]);
          break;
        default:
          stompClient.publish({
            destination: destinations.command(serverId),
            body: command,
          });
      }
    },
    [serverId],
  );

  return { logs, sendCommand };
}
