import { useCallback, useState } from "react";
import { api } from "../lib/apiClient";
import type { FileContent } from "../types/file";

export function useFileEditor() {
  const [file, setFile] = useState<FileContent | null>(null);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const open = useCallback((path: string) => {
    setLoading(true);
    setError(null);
    api.files
      .getContent(path)
      .then((res) => {
        setFile(res);
        setDraft(res.content);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const save = useCallback(() => {
    if (!file) return;
    setSaving(true);
    setError(null);
    return api.files
      .saveContent(file.path, draft)
      .then(() => setFile((f) => (f ? { ...f, content: draft } : f)))
      .catch((err) => setError(err.message))
      .finally(() => setSaving(false));
  }, [file, draft]);

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
