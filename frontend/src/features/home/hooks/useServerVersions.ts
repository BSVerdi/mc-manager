import { useCallback, useEffect, useState } from "react";
import { createServerApi } from "../api";

interface UseServerVersionsResult {
  data: string[];
  isLoading: boolean;
  error: Error | null;
  refetch: () => void;
}

export function useServerVersions(type: string): UseServerVersionsResult {
  const [data, setData] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchVersions = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const versions = await createServerApi.getReleasedVersions(type);
      setData(versions);
    } catch (err) {
      const normalized = err instanceof Error ? err : new Error(String(err));
      setError(normalized);
      console.error("[useServerVersions] Erro ao carregar versões.", normalized);
    } finally {
      setIsLoading(false);
    }
  }, [type]);

  useEffect(() => {
    (async () => {
      await fetchVersions();
    })();
  }, [fetchVersions]);

  return { data, isLoading, error, refetch: fetchVersions };
}
