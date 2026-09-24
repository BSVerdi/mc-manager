import { useCallback, useEffect, useState } from "react";
import { filesApi } from "./api";
import type { FileEntry } from "./types";

export function useFileBrowser(serverId: number) {
  const [currentPath, setCurrentPath] = useState("");
  const [entries, setEntries] = useState<FileEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<unknown>(null);

  const load = useCallback(
    async (path: string) => {
      setLoading(true);
      setError(null);

      try {
        const response = await filesApi.list(serverId, path);
        setEntries(response);
        setCurrentPath(path);
      } catch (err) {
        console.error("[useFileBrowser] Erro ao carregar arquivos:", err);
        setError(err);
      } finally {
        setLoading(false);
      }
    },
    [serverId],
  );

  useEffect(() => {
    load("");
  }, [load]);

  const openFolder = useCallback(
    async (name: string) => {
      const newPath = currentPath ? `${currentPath}/${name}` : name;
      await load(newPath);
    },
    [serverId, currentPath, load],
  );

  const goUp = useCallback(async () => {
    if (!currentPath) return;
    const parts = currentPath.split("/").filter(Boolean);
    parts.pop();
    await load(parts.join("/"));
  }, [currentPath, load]);

  const goToBreadcrumb = useCallback(
    async (index: number) => {
      const parts = currentPath.split("/").filter(Boolean);
      await load(parts.slice(0, index + 1).join("/"));
    },
    [serverId, currentPath, load],
  );

  const upload = useCallback(
    async (file: File) => {
      setError(null);

      try {
        await filesApi.upload(serverId, currentPath, file);
        await load(currentPath);
      } catch (err) {
        console.error("[useFileBrowser] Erro ao realizar upload do arquivo:", err);
        setError(err);
      }
    },
    [serverId, currentPath, load],
  );

  const uploadZip = useCallback(
    async (file: File) => {
      setError(null);

      try {
        await filesApi.uploadZip(serverId, currentPath, file);
      } catch {
        console.error("[useFileBrowser] Erro ao realizar upload de arquivo");
      } finally {
        await load(currentPath);
      }
    },
    [serverId, currentPath, load],
  );

  const remove = useCallback(
    async (path: string) => {
      setError(null);

      try {
        await filesApi.delete(serverId, path);
        await load(currentPath);
      } catch (err) {
        console.error("[useFileBrowser] Erro ao deletar:", err);
      }
    },
    [serverId, currentPath, load],
  );

  const reload = useCallback(() => load(currentPath), [serverId, currentPath, load]);

  const getDownloadUrl = useCallback(
    (path: string, directory: boolean) => {
      if (directory) return filesApi.downloadZipUrl(serverId, path);

      return filesApi.downloadUrl(serverId, path);
    },
    [serverId],
  );

  return {
    currentPath,
    entries,
    loading,
    error,
    openFolder,
    goUp,
    goToBreadcrumb,
    remove,
    reload,
    upload,
    uploadZip,
    getDownloadUrl,
  };
}
