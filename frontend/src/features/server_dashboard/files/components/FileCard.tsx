import { Download, File, FileCog, FileTerminal, FileText, Folder, ImageIcon, Trash2 } from "lucide-react";
import { memo, type ReactNode } from "react";
import type { FileEntry } from "../types";
import { formatSize } from "../utils/formatSize";

const ICONS: Record<string, ReactNode> = {
  "": <Folder style={{ width: 40, height: 40, color: "#3b82f6", fill: "#eff6ff" }} />,

  txt: <FileText style={{ width: 40, height: 40, color: "#6b7280" }} />,

  properties: <FileCog style={{ width: 40, height: 40, color: "#6b7280" }} />,

  bat: <FileTerminal style={{ width: 40, height: 40, color: "#6b7280" }} />,

  jar: <FileTerminal style={{ width: 40, height: 40, color: "#6b7280" }} />,

  png: <ImageIcon style={{ width: 40, height: 40, color: "#a855f7" }} />,
};

const DEFAULT_ICON: ReactNode = <File style={{ width: 40, height: 40, color: "#9ca3af" }} />;

function getFileIcon(extension: string): ReactNode {
  return ICONS[extension] ?? DEFAULT_ICON;
}

interface FileCardProps {
  entry: FileEntry;
  entryPath: string;
  deleting: boolean;
  openFolder: (name: string) => void;
  openFile: (path: string) => void;
  onDelete: (path: string, name: string) => void;
  download: (path: string, directory: boolean, name: string) => Promise<void>;
}

function FileCardBase({ entry, entryPath, deleting, openFolder, openFile, onDelete, download }: FileCardProps) {
  const handleMainClick = () => {
    if (entry.directory) openFolder(entry.name);
    else openFile(entryPath);
  };

  const handleDownload = async () => {
    await download(entryPath, entry.directory, entry.name);
  };

  return (
    <div className="file-card">
      <div className="file-item-main" onClick={handleMainClick}>
        <div className="file-icon">{getFileIcon(entry.extension)}</div>
        <div className="file-details">
          <h3 className="file-title">{entry.name}</h3>
          <div className="file-meta">
            <span>Última modificação: {new Date(entry.lastModified).toLocaleDateString()}</span>
            {formatSize(entry.sizeBytes) && (
              <>
                <span>•</span>
                <span>{formatSize(entry.sizeBytes)}</span>
              </>
            )}
          </div>
        </div>
      </div>
      <div className="file-btns-container">
        <button className="download-btn" onClick={handleDownload} aria-label="Baixar">
          <Download style={{ width: 24, height: 24, color: "#9ca3af" }} />
        </button>
        <button
          className="file-delete-btn"
          onClick={() => onDelete(entryPath, entry.name)}
          aria-label="Excluir"
          disabled={deleting}
        >
          <Trash2 style={{ width: 24, height: 24, color: "#ff3535" }} />
        </button>
      </div>
    </div>
  );
}

export const FileCard = memo(FileCardBase);
