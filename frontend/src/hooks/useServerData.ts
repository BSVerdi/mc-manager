import { useEffect, useState } from "react";
import { api } from "../lib/apiClient";
import type { Server } from "../types/Server";

export function useServerData(serverId: number) {
  const [server, setServer] = useState<Server>();
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    api.servers
      .get(serverId)
      .then(setServer)
      .catch((err) =>
        console.error(
          "[useServerData] Erro ao carregar os dados do servidor.",
          err,
        ),
      )
      .finally(() => setLoading(false));
  }, [serverId]);

  return { server, loading };
}
