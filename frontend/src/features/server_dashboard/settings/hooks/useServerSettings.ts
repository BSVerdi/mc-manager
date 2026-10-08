import { useCallback, useState } from "react";
import type { UpdateServerRequest } from "../../types";
import { serverSettingsApi } from "../api";

export function useServerSettings(serverId: number) {
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState<boolean>(false);

  const updateServer = useCallback(
    async (data: UpdateServerRequest): Promise<boolean> => {
      setSubmitting(true);

      try {
        await serverSettingsApi.update(serverId, data);

        return true;
      } catch (err) {
        console.error("[useServerSettings] Erro ao atualizar os dados do servidor:", err);
        setError("Erro ao atualizar os dados do servidor.");

        return false;
      } finally {
        setSubmitting(false);
      }
    },
    [serverId],
  );

  const deleteServer = useCallback(async (): Promise<boolean> => {
    setSubmitting(true);

    try {
      await serverSettingsApi.delete(serverId);

      return true;
    } catch (err) {
      console.error("[useServerSettings] Erro ao deletar servidor:", err);
      setError("Erro ao deletar servidor.");

      return false;
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
