import { MoveLeft } from "lucide-react";
import { useCallback, useRef, useState } from "react";
import { LoadingScreen } from "../../../../components/LoadingScreen";
import { useToast } from "../../../../context/ToastContext";
import { useConfirm } from "../../../../hooks/useConfirm";
import type { FileExplorerProps } from "../index";
import { zipFolder } from "../utils/zipFolder";
import { FileCard } from "./FileCard";
import { UploadButton } from "./UploadButton";

export function FileBrowser({ browser, editor }: FileExplorerProps) {
  const [deleting, setDeleting] = useState<boolean>(false);
  const [uploading, setUploading] = useState<boolean>(false);

  const deletingRef = useRef(false);
  const { success, error } = useToast();
  const { confirm, modal } = useConfirm();

  const onDelete = useCallback(
    async (path: string, name: string) => {
      if (deletingRef.current) return;

      const confirmed = await confirm({
        title: `Deletar ${name}`,
        description: `Tem certeza que deseja Deletar ${name}?`,
        confirmLabel: "Deletar",
        cancelLabel: "Cancelar",
      });

      if (!confirmed) return;

      deletingRef.current = true;
      setDeleting(true);

      const ok = await browser.remove(path);

      deletingRef.current = false;
      setDeleting(false);

      if (!ok) {
        error(`Erro ao remover ${name}.`, {
          id: "borwser-remove-error",
          duration: 1000,
        });
        return;
      }

      success(`${name} removido com sucesso.`, {
        id: "browser-remove-success",
        duration: 1000,
      });
    },
    [browser.remove],
  );

  const onFileSelected = useCallback(
    async (file: File) => {
      if (uploading) return;

      const result = await browser.upload(file);

      if (!result) {
        error("Erro ao realizar upload do arquivo", {
          id: "browser-upload-error",
          duration: 1000,
        });
        return;
      }

      success("Upload do arquivo realizado com sucesso.", {
        id: "browser-upload-success",
        duration: 1000,
      });
    },
    [browser.upload, uploading],
  );

  const onFolderSelected = useCallback(
    async (fileList: FileList) => {
      if (uploading) return;

      setUploading(true);

      const zipFile = await zipFolder(fileList);
      const result = await browser.uploadZip(zipFile);

      setUploading(false);

      if (!result) {
        error("Erro ao realizar upload do arquivo", {
          id: "browser-uploadZip-error",
          duration: 1000,
        });
        return;
      }

      success("Upload do arquivo realizado com sucesso.", {
        id: "browser-uploadZip-success",
        duration: 1000,
      });
    },
    [browser.uploadZip, uploading],
  );

  const breadcrumbs = browser.currentPath.split("/").filter(Boolean);

  return (
    <>
      <section className="file-grid-container">
        <LoadingScreen isVisible={uploading} />
        <section className="file-grid-header">
          <div>
            <a onClick={() => browser.goToBreadcrumb(-1)}>root</a>
            {breadcrumbs.map((part, i) => (
              <span key={i}>
                <span className="breadcrumb-sep">/</span>
                <a className="breadcrumb-item" onClick={() => browser.goToBreadcrumb(i)}>
                  {part}
                </a>
              </span>
            ))}
          </div>
          <UploadButton disable={uploading} onFileSelected={onFileSelected} onFolderSelected={onFolderSelected} />
        </section>
        <section className="file-grid">
          {browser.currentPath && (
            <div className="file-card walk-back" onClick={browser.goUp}>
              <MoveLeft />
              <h3>..</h3>
            </div>
          )}
          {browser.entries.map((entry) => (
            <FileCard
              key={entry.name}
              entry={entry}
              entryPath={browser.getEntryPath(entry.name)}
              deleting={deleting}
              openFolder={browser.openFolder}
              openFile={editor.open}
              onDelete={onDelete}
              download={browser.download}
            />
          ))}
        </section>
      </section>
      {modal}
    </>
  );
}
