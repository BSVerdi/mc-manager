import { useEffect, useState } from "react";
import { onStompConnect, stompClient, topics } from "../../../lib/stompClient";
import { EMPTY_METRICS, type Metrics } from "./types";

export function useMetricsSocket(serverId: number) {
  const [metrics, setMetrics] = useState<Metrics>(EMPTY_METRICS);

  useEffect(() => {
    setMetrics(EMPTY_METRICS);

    let subscription: ReturnType<typeof stompClient.subscribe> | undefined;

    const unregister = onStompConnect(() => {
      subscription = stompClient.subscribe(topics.metrics(serverId), (message) => {
        try {
          setMetrics(JSON.parse(message.body));
        } catch (err) {
          console.error("[useMetricSocket] JSON invalido: ", err);
        }
      });
    });

    return () => {
      unregister();
      subscription?.unsubscribe();
    };
  }, [serverId]);

  return metrics;
}
