import { useCallback, useState } from "react";
import type { UpdateServerRequest } from "../types";
import { serverSettingsApi } from "./api";

export function useServerSettings(serverId: number) {
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState<boolean>(false);

  const updateServer = useCallback(
    async (data: UpdateServerRequest) => {
      setSubmitting(true);

      try {
        await serverSettingsApi.update(serverId, data);
      } catch (err) {
        console.error("[useServerSettings] Erro ao atualizar os dados do servidor:", err);
        setError("Erro ao atualizar os dados do servidor.");
      } finally {
        setSubmitting(false);
      }
    },
    [serverId],
  );

  const deleteServer = useCallback(async () => {
    setSubmitting(true);

    try {
      await serverSettingsApi.delete(serverId);
    } catch (err) {
      console.error("[useServerSettings] Erro ao deletar servidor:", err);
      setError("Erro ao deletar servidor.");
    } finally {
      setSubmitting(false);
    }
  }, [serverId]);

  return {
    error,
    submitting,
    updateServer,
    deleteServer,
  };
}
