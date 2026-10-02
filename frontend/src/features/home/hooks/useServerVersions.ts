import { useCallback, useEffect, useState } from "react";
import type { ServerVersion } from "../../../types/server";
import { createServerApi } from "../api";

interface UseServerVersionsResult {
  data: ServerVersion[];
  isLoading: boolean;
  error: Error | null;
  refetch: () => void;
}

export function useServerVersions(): UseServerVersionsResult {
  const [data, setData] = useState<ServerVersion[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchVersions = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const versions = await createServerApi.getReleasedVersions();
      setData(versions);
    } catch (err) {
      const normalized = err instanceof Error ? err : new Error(String(err));
      setError(normalized);
      console.error("[useServerVersions] Erro ao carregar versões.", normalized);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;

    (async () => {
      if (!active) return;
      await fetchVersions();
    })();

    return () => {
      active = false;
    };
  }, []);

  return { data, isLoading, error, refetch: fetchVersions };
}
