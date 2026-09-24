import { useCallback, useEffect, useState } from "react";
import { api } from "../lib/apiClient";
import type { Server } from "../types/server";

export function useServerList() {
  const [servers, setServers] = useState<Server[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  const refetchServers = useCallback(async () => {
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

  useEffect(() => {
    refetchServers();
  }, [refetchServers]);

  return { servers, loading, refetchServers };
}
