import { useEffect, useState } from "react";
import { api } from "../lib/apiClient";
import type { Server } from "../types/Server";

export function useServerList() {
  const [servers, setServers] = useState<Server[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    api.servers
      .list()
      .then(setServers)
      .catch((err) => console.error("Erro ao buscar servidores: ", err))
      .finally(() => setLoading(false));
  }, []);

  return { servers, loading };
}
