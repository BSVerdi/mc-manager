import { useCallback, useEffect, useState } from "react";
import { useToast } from "../context/ToastContext";
import { api } from "../lib/apiClient";
import type { Option } from "../types/option";

export function useServerCatalog() {
  const [loading, setLoading] = useState<boolean>(false);
  const [catalog, setCatalog] = useState<Map<Option, Option[]>>(new Map());

  const { error } = useToast();

  const getCatalog = useCallback(async () => {
    setLoading(true);

    try {
      const data = await api.servers.getCatalog();

      setCatalog(
        new Map<Option, Option[]>(
          data.map((c) => [
            {
              value: c.provider,
              label: c.provider.toLocaleUpperCase().charAt(0) + c.provider.slice(1),
            },
            c.versions.map((v) => ({ value: v, label: v })),
          ]),
        ),
      );
    } catch (err) {
      console.error("[useServerCatalog] Erro ao carregar o catalogo", err);
      error("Erro ao carregar o catalogo", {
        id: "server-catalog-error",
        duration: 2000,
      });
    } finally {
      setLoading(false);
    }
  }, [error]);

  useEffect(() => {
    getCatalog();
  }, [getCatalog]);

  return { loading, catalog };
}
