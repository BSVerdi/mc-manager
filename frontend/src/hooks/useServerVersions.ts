import { useEffect, useState } from "react";
import type { ServerVersion } from "../types/Server";
import { api } from "../lib/apiClient";

export function useServerVersions() {
  const [serverVersions, setServerVersions] = useState<ServerVersion[]>([]);

  useEffect(() => {
    api.servers
      .getReleasedVersions()
      .then(setServerVersions)
      .catch((err) =>
        console.error("[useServerVersion] Erro ao carregar as versões"),
      );
  }, []);

  return serverVersions;
}
