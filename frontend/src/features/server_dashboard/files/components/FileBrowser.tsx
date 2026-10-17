import { AnimatePresence, motion } from "framer-motion";
import { MoveLeft, Upload } from "lucide-react";
import { useDelete } from "../hooks/useDelete";
import { useDropZone } from "../hooks/useDropZone";
import { useFileBrowser } from "../hooks/useFileBrowser";
import { useUpload } from "../hooks/useUpload";
import { Breadcrumbs } from "./Breadcrumbs";
import { FileCard } from "./FileCard";
import { Notification } from "./Notification";
import { UploadButton } from "./UploadButton";

interface FileBrowserProps {
  serverId: number;
  openFile: (path: string) => void;
}

export function FileBrowser({ serverId, openFile }: FileBrowserProps) {
  const browser = useFileBrowser(serverId);
  const { uploadState, progress, process, onFileSelected, onFolderSelected } = useUpload(
    serverId,
    browser.currentPath,
    browser.reload,
  );
  const { dragging, onDragOver, onDragEnter, onDragLeaves, onDrop } = useDropZone(process);
  const { deleting, onDelete } = useDelete(browser.remove);

  const breadcrumbs = browser.currentPath.split("/").filter(Boolean);
  const notification = uploadState || dragging;
  const uploading = uploadState === "uploading";

  return (
    <>
      <section className="file-grid-container">
        <section className="file-grid-header">
          <Breadcrumbs goToBreadcrumb={browser.goToBreadcrumb} breadcrumbs={breadcrumbs} />
          <UploadButton disable={uploading} onFileSelected={onFileSelected} onFolderSelected={onFolderSelected} />
        </section>
        <section
          className={dragging ? "file-grid dragging" : "file-grid"}
          onDragOver={onDragOver}
          onDragEnter={onDragEnter}
          onDragLeave={onDragLeaves}
          onDrop={onDrop}
        >
          <AnimatePresence mode="wait">
            {!uploading && dragging && (
              <motion.div
                className="dragging-overlay"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                transition={{ duration: 0.2 }}
              >
                <motion.div
                  className="dragging-upload-icon"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  transition={{ duration: 0.2 }}
                >
                  <Upload size={64} color="#6b7280" />
                </motion.div>
              </motion.div>
            )}
            {notification && <Notification uploadState={uploadState} progress={progress} />}
          </AnimatePresence>
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
              openFile={openFile}
              onDelete={onDelete}
              download={browser.download}
            />
          ))}
        </section>
      </section>
    </>
  );
}
