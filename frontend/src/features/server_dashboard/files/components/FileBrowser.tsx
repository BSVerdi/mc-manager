import { AnimatePresence, motion } from "framer-motion";
import { MoveLeft, Upload } from "lucide-react";
import { LoadingScreen } from "../../../../components/LoadingScreen";
import { Portal } from "../../../../components/Portal";
import { useConfirmDialog } from "../../../../context/ConfirmDialogContext";
import { useToast } from "../../../../context/ToastContext";
import { elevateIn } from "../../../../lib/animations/elevateIn";
import { useDelete } from "../hooks/useDelete";
import { useDropZone } from "../hooks/useDropZone";
import type { FilesBrowserApi } from "../hooks/useFileBrowser";
import { useUpload } from "../hooks/useUpload";
import { Breadcrumbs } from "./Breadcrumbs";
import { FileCard } from "./FileCard";
import { UploadButton } from "./UploadButton";

interface FileBrowserProps {
  browser: FilesBrowserApi;
  openFile: (path: string) => void;
}

export function FileBrowser({ browser, openFile }: FileBrowserProps) {
  const { success, error } = useToast();
  const { confirm } = useConfirmDialog();
  const { uploading, process, onFileSelected, onFolderSelected } = useUpload(
    browser.upload,
    browser.uploadZip,
    success,
    error,
  );
  const { deleting, onDelete } = useDelete(success, error, browser.remove, confirm);
  const { dragging, onDragOver, onDragEnter, onDragLeaves, onDrop } = useDropZone(process);

  const breadcrumbs = browser.currentPath.split("/").filter(Boolean);

  return (
    <>
      <section className="file-grid-container">
        {uploading && <LoadingScreen />}
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
          <AnimatePresence>
            {dragging && (
              <>
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
                <Portal>
                  <div className="dragging-notification-container">
                    <motion.div
                      className="dragging-notification"
                      variants={elevateIn}
                      initial="hidden"
                      animate="visible"
                      exit="exit"
                    >
                      <Upload size={32} color="#6b7280" />
                      <span>Solte os arquivos para fazer upload</span>
                    </motion.div>
                  </div>
                </Portal>
              </>
            )}
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
              onDownloadError={(fileName) =>
                error(`Erro ao baixar ${fileName}`, { id: "Download-error", duration: 1000 })
              }
            />
          ))}
        </section>
      </section>
    </>
  );
}
