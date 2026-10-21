import { useCallback, useEffect, useSyncExternalStore } from "react";
import { destinations, onStompConnect, stompClient, topics } from "../../../../lib/stompClient";
import type { Log } from "../types";
import { consoleLogStore } from "./consoleLogStore";

export function useConsoleSocket(serverId: number) {
  const logs = useSyncExternalStore(
    (cb) => consoleLogStore.subscribe(cb),
    () => consoleLogStore.getLogs(serverId),
  );

  useEffect(() => {
    let subscription: ReturnType<typeof stompClient.subscribe> | undefined;

    const unregister = onStompConnect(() => {
      subscription?.unsubscribe();
      subscription = stompClient.subscribe(topics.console(serverId), (message) => {
        try {
          const log: Log = JSON.parse(message.body);
          consoleLogStore.push(serverId, log);
        } catch (err) {
          console.error("[useConsoleSocket] JSON inválido: ", err);
        }
      });
    });

    return () => {
      unregister();
      subscription?.unsubscribe();
    };
  }, [serverId]);

  const sendCommand = useCallback(
    async (command: string) => {
      const normalized = command.trim();

      if (normalized === "") return;

      if (normalized.toLocaleLowerCase() === "clear") {
        consoleLogStore.clear(serverId);
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
