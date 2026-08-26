import { useCallback, useState } from "react";
import { api } from "../lib/apiClient";

export function useServerControl(serverId: number) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const run = useCallback(async (action: () => Promise<unknown>) => {
    setLoading(true);
    setError(null);

    try {
      await action();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setLoading(false);
    }
  }, []);

  return {
    loading,
    error,
    start: () => run(() => api.server.start(serverId)),
    stop: () => run(() => api.server.stop(serverId)),
    restart: () => run(() => api.server.restart(serverId)),
  };
}
