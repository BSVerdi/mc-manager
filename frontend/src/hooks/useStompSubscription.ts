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

    // const trySubscribe = () => {
    //   const subscription = stompClient.subscribe(topic, (message) => {
    //     setData(JSON.parse(message.body));
    //   });

    //   return subscription;
    // };

    // let subscription: ReturnType<typeof stompClient.subscribe> | undefined;

    // stompClient.onConnect = () => {
    //   subscription = trySubscribe();
    // };

    // if (stompClient.connected) {
    //   subscription = trySubscribe();
    // } else {
    //   const originalOnConnect = stompClient.onConnect;

    //   stompClient.onConnect = (frame) => {
    //     originalOnConnect?.(frame);

    //     subscription = trySubscribe();
    //   };
    // }

    // return () => subscription?.unsubscribe();
  }, [topic]);

  return data;
}
