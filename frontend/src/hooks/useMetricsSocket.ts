import { EMPTY_METRICS, type Metrics } from "../types/Metrics";
import { useStompSubscription } from "./useStompSubscription";

export function useMetricsSocket() {
  return useStompSubscription<Metrics>("/topic/metrics", EMPTY_METRICS);
}
