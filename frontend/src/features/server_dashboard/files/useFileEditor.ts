import { useCallback, useState } from "react";
import { filesApi } from "./api";
import type { FileContent } from "./types";

export function useFileEditor(serverId: number) {
  const [file, setFile] = useState<FileContent | null>(null);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const open = useCallback(
    async (path: string) => {
      setLoading(true);
      setError(null);

      try {
        const response = await filesApi.getContent(serverId, path);

        setFile(response);
        setDraft(response.content);
      } catch (err) {
        console.error("[useFileEditor] Erro ao abrir arquivo:", err);
        setError(err);
      } finally {
        setLoading(false);
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
      setError(err);
    } finally {
      setSaving(false);
    }
  }, [file, draft, serverId]);

  const close = useCallback(() => {
    setFile(null);
    setDraft("");
    setError(null);
  }, []);

  const isDirty = file !== null && draft !== file.content;

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
