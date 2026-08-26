import { useCallback, useEffect, useState } from "react";
import { api } from "../lib/apiClient";
import type { FileEntry } from "../types/File";

export function useFileBrowser(serverId: number) {
  const [currentPath, setCurrentPath] = useState("");
  const [entries, setEntries] = useState<FileEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    (path: string) => {
      setLoading(true);
      setError(null);
      api.files
        .list(serverId, path)
        .then((res) => {
          setEntries(res);
          setCurrentPath(path);
        })
        .catch((err) => setError(err.message))
        .finally(() => setLoading(false));
    },
    [serverId],
  );

  useEffect(() => {
    load("");
  }, [serverId]);

  const openFolder = useCallback(
    (name: string) => {
      const newPath = currentPath ? `${currentPath}/${name}` : name;
      load(newPath);
    },
    [currentPath, load],
  );

  const goUp = useCallback(() => {
    if (!currentPath) return;
    const parts = currentPath.split("/").filter(Boolean);
    parts.pop();
    load(parts.join("/"));
  }, [currentPath, load]);

  const goToBreadcrumb = useCallback(
    (index: number) => {
      const parts = currentPath.split("/").filter(Boolean);
      load(parts.slice(0, index + 1).join("/"));
    },
    [currentPath, load],
  );

  const upload = useCallback(
    (file: File) => {
      setError(null);

      return api.files
        .upload(serverId, currentPath, file)
        .then(() => load(currentPath))
        .catch((err) => {
          setError(err.response?.data?.error ?? err.message);
          throw err;
        });
    },
    [currentPath, load],
  );

  const uploadZip = useCallback(
    (file: File) => {
      setError(null);

      return api.files
        .uploadZip(serverId, currentPath, file)
        .catch((err) => {
          setError(err.response?.data?.error ?? err.message);
          throw err;
        })
        .finally(() => window.location.reload());
    },
    [currentPath],
  );

  return {
    currentPath,
    entries,
    loading,
    error,
    openFolder,
    goUp,
    goToBreadcrumb,
    reload: () => load(currentPath),
    upload,
    uploadZip,
  };
}
