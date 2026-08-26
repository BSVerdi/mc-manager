import { useEffect, useState } from "react";
import { onStompConnect, stompClient } from "../lib/stompClient";

export function useStompSubscription<T>(topic: string, initialValue: T): T {
  const [data, setData] = useState<T>(initialValue);

  useEffect(() => {
    let subscription: ReturnType<typeof stompClient.subscribe> | undefined;

    const unregister = onStompConnect(() => {
      subscription = stompClient.subscribe(topic, (message) => {
        try {
          setData(JSON.parse(message.body));
        } catch (err) {
          console.error(err);
        }
      });
    });

    return () => {
      unregister();
      subscription?.unsubscribe();
    };
  }, [topic]);

  return data;
}
