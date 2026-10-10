import { useCallback, useEffect, useState } from "react";
import { api } from "../lib/apiClient";
import type { Server } from "../types/server";

export function useServerList() {
  const [servers, setServers] = useState<Server[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const getServers = useCallback(async () => {
    setLoading(true);

    try {
      const response = await api.servers.list();
      setServers(response);
    } catch (err) {
      console.error("Erro ao listar servidores:", err);
    } finally {
      setLoading(false);
    }
  }, []);

  const reload = useCallback(() => getServers(), [getServers]);

  useEffect(() => {
    getServers();
  }, [getServers]);

  return { servers, loading, getServers, reload };
}
