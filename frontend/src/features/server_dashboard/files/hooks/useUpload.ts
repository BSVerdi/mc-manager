import { useCallback, useRef, useState } from "react";
import type { ToastOptions } from "../../../../types/toast";
import type { Payload } from "../types";
import { fileListToEntries, zipEntries } from "../utils/zipFolder";

export function useUpload(
  upload: (file: File) => Promise<boolean>,
  uploadZip: (file: File) => Promise<boolean>,
  success: (message: string, options: ToastOptions) => void,
  error: (message: string, options: ToastOptions) => void,
) {
  const [uploading, setUploading] = useState<boolean>(false);
  const uploadingRef = useRef(false);

  const process = useCallback(
    async (payload: Payload) => {
      if (uploadingRef.current) return;
      uploadingRef.current = true;
      setUploading(true);

      try {
        if (payload.kind === "single") {
          const ok = await upload(payload.entry.file);
        } else {
          const zipFile = await zipEntries(payload.entries, payload.zipName);

          const ok = await uploadZip(zipFile);

          if (ok) {
            success("Upload da pasta realizado com sucesso.", {
              id: "browser-uploadZip-success",
              duration: 1000,
            });
          } else {
            error("Erro ao realizar upload da pasta", {
              id: "browser-upload-error",
              duration: 1000,
            });
          }
        }
      } catch {
        error("Erro ao realizar upload.", {
          id: "browser-upload-error",
          duration: 1000,
        });
      } finally {
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
