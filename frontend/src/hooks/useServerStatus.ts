import { useCallback, useEffect, useState } from "react";
import { api } from "../lib/apiClient";
import { Status } from "../types/status";

export function useServerStatus(serverId: number) {
  const [status, setStatus] = useState<Status>(Status.OFFLINE);
  const [loading, setLoading] = useState<boolean>(false);

  const getStatus = useCallback(async () => {
    setLoading(true);

    try {
      const response = await api.server.status(serverId);
      setStatus(response.status);
    } catch (err) {
      console.error("[useServerStatus] Erro ao carregar o status do servidor.", err);
    } finally {
      setLoading(false);
    }
  }, [serverId]);

  const reload = useCallback(() => getStatus(), [getStatus]);

  useEffect(() => {
    getStatus();
  }, [serverId]);

  return { status, loading, getStatus, reload };
}
