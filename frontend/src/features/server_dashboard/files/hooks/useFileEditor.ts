import { useCallback, useRef, useState } from "react";
import type { ConfirmInput } from "../../../../types/confirm";
import { filesApi } from "../api";
import type { FileContent } from "../types";

export interface FileEditorApi {
  file: FileContent | null;
  draft: string;
  setDraft: (v: string) => void;
  open: (path: string) => void;
  save: () => Promise<void>;
  close: () => Promise<void>;
  isDirty: boolean;
  loading: boolean;
  saving: boolean;
  error: string | null;
}

export function useFileEditor(serverId: number, confirm: (next: ConfirmInput) => Promise<boolean>): FileEditorApi {
  const [file, setFile] = useState<FileContent | null>(null);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const openRequestIdRef = useRef(0);
  const isDirty = file !== null && draft !== file.content;

  const open = useCallback(
    async (path: string) => {
      const id = ++openRequestIdRef.current;

      setLoading(true);
      setError(null);

      try {
        const response = await filesApi.getContent(serverId, path);

        if (id !== openRequestIdRef.current) return;

        setFile(response);
        setDraft(response.content);
      } catch (err) {
        if (id !== openRequestIdRef.current) return;

        console.error("[useFileEditor] Erro ao abrir arquivo:", err);
        setError("Erro ao abrir arquivo.");
      } finally {
        if (id === openRequestIdRef.current) setLoading(false);
      }
    },
    [serverId],
  );

  const save = useCallback(async () => {
    if (!file) return;

    setSaving(true);
    setError(null);

    try {
      await filesApi.saveContent(serverId, file.path, draft);

      setFile((f) => (f ? { ...f, content: draft } : f));
    } catch (err) {
      console.error("[useFileEditor] Erro ao salvar conteúdo do arquivo:", err);
      setError("Erro ao salvar conteudo do arquivo.");
    } finally {
      setSaving(false);
    }
  }, [file, draft, serverId]);

  const close = useCallback(async () => {
    if (isDirty) {
      const confirmed = await confirm({
        title: "Descartar Alterações",
        description: "Deseja descartar as alterações feitas?",
        confirmLabel: "Descartar",
        cancelLabel: "Cancelar",
      });

      if (!confirmed) return;
    }

    setFile(null);
    setDraft("");
    setError(null);
  }, [isDirty, confirm]);

  return {
    file,
    draft,
    setDraft,
    open,
    save,
    close,
    isDirty,
    loading,
    saving,
    error,
  };
}
