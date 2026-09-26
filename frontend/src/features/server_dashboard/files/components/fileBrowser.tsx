import { MoveLeft } from "lucide-react";
import { useCallback, useRef, useState } from "react";
import LoadingScreen from "../../../../components/LoadingScreen";
import type { FileExplorerProps } from "../index";
import { zipFolder } from "../utils/zipFolder";
import FileCard from "./FileCard";
import UploadButton from "./UploadButton";

export function FileBrowser({ browser, editor }: FileExplorerProps) {
  const [deleting, setDeleting] = useState<boolean>(false);
  const [zipping, setZipping] = useState<boolean>(false);
  const deletingRef = useRef(false);

  const onDelete = useCallback(
    async (path: string, name: string) => {
      if (deletingRef.current) return;

      if (!window.confirm(`Tem certeza que deseja excluir "${name}"`)) return;

      deletingRef.current = true;
      setDeleting(true);

      await browser.remove(path);

      deletingRef.current = false;
      setDeleting(false);
    },
    [browser.remove],
  );

  const onFileSelected = useCallback(
    async (file: File) => {
      await browser.upload(file);
    },
    [browser.upload],
  );

  const onFolderSelected = useCallback(
    async (fileList: FileList) => {
      setZipping(true);

      const zipFile = await zipFolder(fileList);
      await browser.uploadZip(zipFile);

      setZipping(false);
    },
    [browser.uploadZip],
  );

  const breadcrumbs = browser.currentPath.split("/").filter(Boolean);

  return (
    <section className="file-grid-container">
      <LoadingScreen isVisible={zipping} />
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
        <UploadButton disable={zipping} onFileSelected={onFileSelected} onFolderSelected={onFolderSelected} />
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
  );
}
