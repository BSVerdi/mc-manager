import { useEffect, useState } from "react";
import { api } from "../lib/apiClient";
import type { ServerVersion } from "../types/server";

export function useServerVersions() {
  const [serverVersions, setServerVersions] = useState<ServerVersion[]>([]);

  useEffect(() => {
    api.servers
      .getReleasedVersions()
      .then(setServerVersions)
      .catch((err) =>
        console.error("[useServerVersion] Erro ao carregar as versões.", err),
      );
  }, []);

  return serverVersions;
}
