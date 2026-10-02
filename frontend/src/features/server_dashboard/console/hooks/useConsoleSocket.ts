import { useCallback, useEffect, useState } from "react";
import { destinations, onStompConnect, onStompDisconnect, stompClient, topics } from "../../../../lib/stompClient";
import type { Log } from "../types";

const MAX_LINES = 2000;

export function useConsoleSocket(serverId: number) {
  const [logs, setLogs] = useState<Log[]>([]);

  useEffect(() => {
    setLogs([]);

    let subscription: ReturnType<typeof stompClient.subscribe> | undefined;

    const unregister = onStompConnect(() => {
      subscription?.unsubscribe();
      subscription = stompClient.subscribe(topics.console(serverId), (message) => {
        try {
          const log: Log = JSON.parse(message.body);
          setLogs((old) => {
            const updated = [...old, log];
            return updated.length > MAX_LINES ? updated.slice(-MAX_LINES) : updated;
          });
        } catch (err) {
          console.error("[useConsoleSocket] JSON inválido: ", err);
        }
      });
    });

    const offDisconnect = onStompDisconnect(() => setLogs([]));

    return () => {
      unregister();
      offDisconnect();
      subscription?.unsubscribe();
    };
  }, [serverId]);

  const sendCommand = useCallback(
    async (command: string) => {
      const normalized = command.trim();

      if (normalized === "") return;

      if (normalized.toLocaleLowerCase() === "clear") {
        setLogs([]);
        return;
      }

      if (!stompClient.connected) {
        console.warn("[useConsoleSocket] Cliente STOMP desconectado -- comando não enviado.");
        return;
      }

      stompClient.publish({
        destination: destinations.command(serverId),
        body: command,
      });
    },
    [serverId],
  );

  return { logs, sendCommand };
}
