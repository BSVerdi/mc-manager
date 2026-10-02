import { useCallback, useEffect, useRef, useState } from "react";
import { filesApi } from "../api";
import type { FileEntry } from "../types";

export interface FilesBrowserApi {
  currentPath: string;
  entries: FileEntry[];
  loading: boolean;
  error: string | null;
  openFolder: (name: string) => void;
  goUp: () => void;
  goToBreadcrumb: (index: number) => void;
  getEntryPath: (name: string) => string;
  remove: (path: string) => Promise<boolean>;
  reload: () => void;
  upload: (file: File) => Promise<boolean>;
  uploadZip: (file: File) => Promise<boolean>;
  download: (path: string, directory: boolean, name: string) => Promise<void>;
}

export function useFileBrowser(serverId: number): FilesBrowserApi {
  const [currentPath, setCurrentPath] = useState("");
  const [entries, setEntries] = useState<FileEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const requestIdRef = useRef(0);

  const load = useCallback(
    async (path: string) => {
      const id = ++requestIdRef.current;

      setLoading(true);
      setError(null);

      try {
        const response = await filesApi.list(serverId, path);

        if (id !== requestIdRef.current) return;

        setEntries(response);
        setCurrentPath(path);
      } catch (err) {
        if (id !== requestIdRef.current) return;

        console.error("[useFileBrowser] Erro ao carregar arquivos:", err);
        setError("Erro ao carregar arquivos");
      } finally {
        if (id === requestIdRef.current) setLoading(false);
      }
    },
    [serverId],
  );

  useEffect(() => {
    load("");
  }, [load]);

  const getEntryPath = useCallback((name: string) => (currentPath ? `${currentPath}/${name}` : name), [currentPath]);

  const openFolder = useCallback(
    async (name: string) => {
      await load(getEntryPath(name));
    },
    [getEntryPath, load],
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
    [currentPath, load],
  );

  const upload = useCallback(
    async (file: File): Promise<boolean> => {
      setError(null);

      try {
        await filesApi.upload(serverId, currentPath, file);

        return true;
      } catch (err) {
        console.error("[useFileBrowser] Erro ao realizar upload do arquivo:", err);
        setError("Erro ao realizar upload de arquivo");

        return false;
      } finally {
        await load(currentPath);
      }
    },
    [serverId, currentPath, load],
  );

  const uploadZip = useCallback(
    async (file: File) => {
      setError(null);

      try {
        await filesApi.uploadZip(serverId, currentPath, file);

        return true;
      } catch {
        console.error("[useFileBrowser] Erro ao realizar upload de arquivo");
        setError("Erro ao realizar upload de pasta.");

        return false;
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

        return true;
      } catch (err) {
        console.error("[useFileBrowser] Erro ao deletar:", err);
        setError("Erro ao deletar arquivo ou diretório.");

        return false;
      } finally {
        await load(currentPath);
      }
    },
    [serverId, currentPath, load],
  );

  const reload = useCallback(() => load(currentPath), [serverId, currentPath, load]);

  const download = useCallback(
    (path: string, directory: boolean, name: string) => {
      if (directory) return filesApi.downloadZip(serverId, path, name);

      return filesApi.download(serverId, path, name);
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
    getEntryPath,
    remove,
    reload,
    upload,
    uploadZip,
    download,
  };
}
