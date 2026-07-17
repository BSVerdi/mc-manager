import { useEffect, useState } from "react";
import { api } from "../lib/apiClient";
import { defaultStatus, type Status } from "../types/status";
import { onStompConnect, stompClient } from "../lib/stompClient";

export function useStatusSocket() {
  const [status, setStatus] = useState<Status>(defaultStatus);

  useEffect(() => {
    api.server
      .status()
      .then((res) => setStatus(res.status))
      .catch((err) => console.error(err));

    let subscription: ReturnType<typeof stompClient.subscribe> | undefined;

    const unregister = onStompConnect(() => {
      subscription = stompClient.subscribe("/topic/status", (message) => {
        try {
          setStatus(JSON.parse(message.body));
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

  return status;
}
