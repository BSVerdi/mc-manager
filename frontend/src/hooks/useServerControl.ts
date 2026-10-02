import { useCallback, useState } from "react";
import { api } from "../lib/apiClient";

export function useServerControl(serverId: number) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const start = useCallback(async () => {
    setLoading(true);

    try {
      await api.server.start(serverId);
    } catch (err) {
      console.error("[useServerControl] Erro ao iniciar servidor:", err);
      setError("Erro ao iniciar servidor.");
    } finally {
      setLoading(false);
    }
  }, [serverId]);

  const stop = useCallback(async () => {
    setLoading(true);

    try {
      await api.server.stop(serverId);
    } catch (err) {
      console.error("[useServerControl] Erro ao parar servidor:", err);
      setError("Erro ao iniciar servidor");
    } finally {
      setLoading(false);
    }
  }, [serverId]);

  const restart = useCallback(async () => {
    setLoading(true);

    try {
      await api.server.restart(serverId);
    } catch (err) {
      console.error("[useServerControl] Erro ao reiniciar servidor:", err);
      setError("Erro ao reiniciar servidor");
    } finally {
      setLoading(false);
    }
  }, [serverId]);

  return {
    loading,
    error,
    start,
    stop,
    restart,
  };
}
