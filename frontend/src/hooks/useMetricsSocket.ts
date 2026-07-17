import { EMPTY_METRICS, type Metrics } from "../types/metrics";
import { useStompSubscription } from "./useStompSubscription";

export function useMetricsSocket() {
  return useStompSubscription<Metrics>("/topic/metrics", EMPTY_METRICS);
}
