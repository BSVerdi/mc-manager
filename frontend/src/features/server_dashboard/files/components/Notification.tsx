import { motion } from "framer-motion";
import { Upload } from "lucide-react";
import { Portal } from "../../../../components/Portal";
import { elevateIn } from "../../../../lib/animations/elevateIn";
import type { UploadState } from "../types";

interface NotificationProps {
  uploadState: UploadState;
  progress: number | null;
}

export function Notification({ uploadState, progress }: NotificationProps) {
  const uploading = uploadState === "uploading";
  const zipping = uploadState === "zipping";

  return (
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
          {!(uploading || zipping) && <span>Solte os arquivos para fazer upload</span>}
          {zipping ? (
            <div className="upload-zipping">
              <span>Comprimindo...</span>
              <div className="track">
                <div className="bar"></div>
              </div>
            </div>
          ) : (
            uploading && (
              <div
                className="upload-progress-bar"
                role="progressbar"
                aria-valuenow={progress ?? 0}
                aria-valuemin={0}
                aria-valuemax={100}
              >
                <span>Enviando... {progress ?? 0}%</span>
                <div className="upload-progress-track">
                  <div className="upload-progress-fill" style={{ width: `${progress ?? 0}%` }} />
                </div>
              </div>
            )
          )}
        </motion.div>
      </div>
    </Portal>
  );
}
