import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { api } from "../lib/apiClient";
import type { Server } from "../types/server";

interface ServersContextValue {
  servers: Server[];
  loading: boolean;
  getServers: () => void;
  reload: () => void;
}

const ServersContext = createContext<ServersContextValue | null>(null);

interface ServersProviderProps {
  children: ReactNode;
}

export function ServersProvider({ children }: ServersProviderProps) {
  const [servers, setServers] = useState<Server[]>([]);
  const [loading, setLoading] = useState<boolean>(false);

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

  useEffect(() => {
    getServers();
  }, [getServers]);

  const reload = useCallback(() => getServers(), [getServers]);

  const value = useMemo(() => ({ servers, loading, getServers, reload }), [servers, loading, getServers, reload]);

  return <ServersContext.Provider value={value}>{children}</ServersContext.Provider>;
}

export function useServers() {
  const context = useContext(ServersContext);

  if (!context) {
    throw new Error("useServers deve ser usado dentro de <SeversProvider/>");
  }

  return context;
}
