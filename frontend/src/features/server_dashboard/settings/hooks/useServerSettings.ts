import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useServers } from "../../../../context/ServersContext";
import { useToast } from "../../../../context/ToastContext";
import type { UpdateServerRequest } from "../../types";
import { serverSettingsApi } from "../api";

export function useServerSettings(serverId: number) {
  const [submitting, setSubmitting] = useState<boolean>(false);

  const navigate = useNavigate();
  const { error, success } = useToast();
  const { reload } = useServers();

  const updateServer = useCallback(
    async (data: UpdateServerRequest): Promise<void> => {
      setSubmitting(true);

      try {
        await serverSettingsApi.update(serverId, data);
        success("Dados do servidor atualizado com sucesso!", {
          id: "server-settings-success",
          duration: 2000,
        });
      } catch (err) {
        console.error("[useServerSettings] Erro ao atualizar os dados do servidor:", err);
        error("Erro ao atualizar os dados do servidor.", {
          id: "server-settings-error",
          duration: 2000,
        });
      } finally {
        setSubmitting(false);
      }
    },
    [serverId],
  );

  const deleteServer = useCallback(async (): Promise<void> => {
    setSubmitting(true);

    try {
      await serverSettingsApi.delete(serverId);
      success("Servidor deletado com sucesso!", {
        id: "server-settings-delete-success",
        duration: 2000,
      });

      reload();
      navigate("/");
    } catch (err) {
      console.error("[useServerSettings] Erro ao deletar servidor:", err);
      error("Erro ao deletar servidor.", {
        id: "server-settings-delete-error",
        duration: 2000,
      });
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
