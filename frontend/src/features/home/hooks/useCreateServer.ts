import { useCallback, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useServers } from "../../../context/ServersContext";
import { useToast } from "../../../context/ToastContext";
import { createServerApi } from "../api";
import type { CreateServerRequest } from "../types";

interface UseCreateServerApi {
  submitting: boolean;
  createServer: (data: CreateServerRequest) => Promise<void>;
}

export function useCreateServer(): UseCreateServerApi {
  const [submitting, setSubmitting] = useState<boolean>(false);

  const { success, error } = useToast();
  const { reload } = useServers();
  const navigate = useNavigate();

  const createServer = useCallback(async (data: CreateServerRequest): Promise<void> => {
    setSubmitting(true);

    try {
      const response = await createServerApi.create(data);

      success("Servidor criado com sucesso", {
        id: "create-server-success",
        duration: 2000,
      });

      await reload();
      navigate(`/servers/${response.id}`);
    } catch (err) {
      console.error("[useCreateServer] Erro ao criar servidor:", err);
      error("Erro ao criar servidor", {
        id: "create-server-error",
        duration: 2000,
      });
    } finally {
      setSubmitting(false);
    }
  }, []);

  return { submitting, createServer };
}
