import { useCallback, useRef, useState } from "react";
import { useToast } from "../../../../context/ToastContext";
import { filesApi } from "../api";
import type { Payload } from "../types";
import { fileListToEntries, zipEntries } from "../utils/zipFolder";

export function useUpload(serverId: number, currentPath: string, reload: () => Promise<void>) {
  const [uploading, setUploading] = useState<boolean>(false);
  const uploadingRef = useRef(false);

  const { success, error } = useToast();

  const upload = useCallback(
    async (file: File): Promise<void> => {
      try {
        await filesApi.upload(serverId, currentPath, file);

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
      }
    },
    [serverId, currentPath, reload],
  );

  const uploadZip = useCallback(
    async (file: File): Promise<void> => {
      try {
        await filesApi.uploadZip(serverId, currentPath, file);

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
      }
    },
    [serverId, currentPath, reload],
  );

  const process = useCallback(
    async (payload: Payload) => {
      if (uploadingRef.current) return;
      uploadingRef.current = true;
      setUploading(true);

      if (payload.kind === "single") {
        await upload(payload.entry.file);
        uploadingRef.current = false;
        setUploading(false);
        return;
      }

      try {
        const zipFile = await zipEntries(payload.entries, payload.zipName);
        await uploadZip(zipFile);
      } catch (err) {
        console.error("[useUpload] Erro ao zippar a pasta");
        error("Erro ao zippar a pasta.", {
          id: "browser-zipping-error",
          duration: 1000,
        });
      } finally {
        uploadingRef.current = false;
        setUploading(false);
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

  return { uploading, process, onFileSelected, onFolderSelected };
}
