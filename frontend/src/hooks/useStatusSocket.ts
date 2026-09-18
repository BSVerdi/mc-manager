import { useEffect, useState } from "react";
import { api } from "../lib/apiClient";
import { defaultStatus, type Status } from "../types/status";
import { onStompConnect, stompClient, topics } from "../lib/stompClient";

export function useStatusSocket(serverId: number) {
  const [status, setStatus] = useState<Status>(defaultStatus);

  useEffect(() => {
    setStatus(defaultStatus);

    api.server
      .status(serverId)
      .then((res) => setStatus(res.status))
      .catch((err) =>
        console.error("[useStatusSocket] Erro ao buscar status inicial: ", err),
      );

    let subscription: ReturnType<typeof stompClient.subscribe> | undefined;

    const unregister = onStompConnect(() => {
      subscription = stompClient.subscribe(
        topics.status(serverId),
        (message) => {
          try {
            setStatus(JSON.parse(message.body));
          } catch (err) {
            console.error("[useStatusSocket] JSON inválido: ", err);
          }
        },
      );
    });

    return () => {
      unregister();
      subscription?.unsubscribe();
    };
  }, [serverId]);

  return status;
}
