import { useEffect, useState } from "react";
import type { Server } from "../types/Server";
import { api } from "../lib/apiClient";

export function useServerData(serverId: number) {
  const [server, setServer] = useState<Server>();
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    api.servers
      .get(serverId)
      .then(setServer)
      .catch((err) =>
        console.error("[useServerData] Erro ao carregar os dados do servidor."),
      )
      .finally(() => setLoading(false));
  }, [serverId]);

  return { server, loading };
}
