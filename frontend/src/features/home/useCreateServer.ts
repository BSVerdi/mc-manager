import { useState } from "react";
import { createServerApi } from "./api";
import type { CreateServerRequest } from "./types";

export function useCreateServer() {
  const [submitting, setSubmitting] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const createServer = async (data: CreateServerRequest): Promise<number | null> => {
    setSubmitting(true);

    try {
      const response = await createServerApi.create(data);

      return response.id;
    } catch (err) {
      console.error("[useCreateServer] Erro ao criar servidor:", err);
      setError("Erro ao criar servidor.");
      return null;
    } finally {
      setSubmitting(false);
    }
  };

  return { submitting, error, createServer };
}
