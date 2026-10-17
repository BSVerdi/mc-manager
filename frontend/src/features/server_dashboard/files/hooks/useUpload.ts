import { useCallback, useState } from "react";
import { useToast } from "../../../../context/ToastContext";
import { filesApi } from "../api";
import type { Payload, UploadState } from "../types";
import { fileListToEntries, zipEntries } from "../utils/zipFolder";

export function useUpload(serverId: number, currentPath: string, reload: () => Promise<void>) {
  const [uploadState, setUploadState] = useState<UploadState>(null);
  const [progress, setProgress] = useState<number | null>(null);

  const { success, error } = useToast();

  const upload = useCallback(
    async (file: File, onProgress?: (pct: number) => void): Promise<void> => {
      setUploadState("uploading");

      try {
        await filesApi.upload(serverId, currentPath, file, onProgress);

        success("Upload do arquivo realizado com sucesso.", {
          id: "browser-upload-success",
          duration: 1000,
        });
      } catch (err) {
        console.error("[useFileBrowser] Erro ao realizar upload do arquivo:", err);
        error("Erro ao realizar upload do arquivo", {
          id: "browser-upload-error",
          duration: 1000,
        });
      } finally {
        await reload();
        setUploadState(null);
      }
    },
    [serverId, currentPath, reload],
  );

  const uploadZip = useCallback(
    async (file: File, onProgress?: (pct: number) => void): Promise<void> => {
      setUploadState("uploading");

      try {
        await filesApi.uploadZip(serverId, currentPath, file, onProgress);

        success("Upload da pasta realizado com sucesso.", {
          id: "browser-upload-success",
          duration: 1000,
        });
      } catch (err) {
        console.error("[useFileBrowser] Erro ao realizar upload da pasta", err);
        error("Erro ao realizar upload da pasta", {
          id: "browser-upload-error",
          duration: 1000,
        });
      } finally {
        await reload();
        setUploadState(null);
      }
    },
    [serverId, currentPath, reload],
  );

  const process = useCallback(
    async (payload: Payload) => {
      if (uploadState) return;

      setProgress(null);

      const onProgress = (pct: number) => setProgress(pct);

      if (payload.kind === "single") {
        await upload(payload.entry.file, onProgress);
        return;
      }

      try {
        setUploadState("zipping");
        const zipFile = await zipEntries(payload.entries, payload.zipName);
        setUploadState(null);

        await uploadZip(zipFile, onProgress);
      } catch (err) {
        console.error("[useUpload] Erro ao zippar a pasta");
        error("Erro ao zippar a pasta.", {
          id: "browser-zipping-error",
          duration: 1000,
        });
      } finally {
        setProgress(null);
      }
    },
    [upload, uploadZip],
  );

  const onFileSelected = useCallback(
    (file: File) => {
      void process({ kind: "single", entry: { file, path: file.name } });
    },
    [process],
  );

  const onFolderSelected = useCallback(
    (fileList: FileList) => {
      const entries = fileListToEntries(fileList);
      const root = entries[0]?.path.split("/")[0] ?? "upload";

      void process({ kind: "bundle", entries, zipName: `${root}.zip` });
    },
    [process],
  );

  return { uploadState, progress, process, onFileSelected, onFolderSelected };
}
